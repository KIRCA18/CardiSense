import importlib.util
import os
from fractions import Fraction
from pathlib import Path

import numpy as np
import torch
from scipy.signal import resample_poly

from app.adapters.base import ModelAdapter
from app.schemas import CommonEcgInput, InferenceResponse, InputSummary, ModelInfo, Prediction


class EcgFounderAdapter(ModelAdapter):
    model_id = "ecg-founder"
    name = "ECGFounder"
    required_leads = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]
    sampling_rate_hz = 500
    segment_seconds = 10
    samples_per_segment = 5000
    accepts_other_sampling_rates = True

    def __init__(self) -> None:
        root = Path(__file__).resolve().parents[2]
        self.repo_path = Path(os.getenv("ECG_FOUNDER_REPO", str(root / "vendor" / "ECGFounder"))).expanduser()
        self.checkpoint_path = Path(os.getenv("ECG_FOUNDER_CHECKPOINT", str(root / "ckpts" / "12_lead_ECGFounder.pth"))).expanduser()
        self.device = torch.device(os.getenv("ECG_FOUNDER_DEVICE", "cuda" if torch.cuda.is_available() else "cpu"))
        self.model = None
        self.labels: list[str] = []
        self.load_error = None

    @property
    def ready(self) -> bool:
        return self.model is not None and self.load_error is None

    def load(self) -> None:
        try:
            network_path = self.repo_path / "net1d.py"
            if not network_path.is_file():
                raise FileNotFoundError(f"Clone ECGFounder into {self.repo_path}; see model-service/README.md")
            if not self.checkpoint_path.is_file():
                raise FileNotFoundError(f"Download the official checkpoint to {self.checkpoint_path}; see model-service/README.md")

            self.labels = (self.repo_path / "tasks.txt").read_text(encoding="utf-8").splitlines()
            if len(self.labels) != 150 or any(not label.strip() for label in self.labels):
                raise ValueError("ECGFounder tasks.txt must contain exactly 150 nonempty labels")

            spec = importlib.util.spec_from_file_location("cardisense_ecg_founder_net", network_path)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)
            model = module.Net1D(
                in_channels=12, base_filters=64, ratio=1,
                filter_list=[64, 160, 160, 400, 400, 1024, 1024],
                m_blocks_list=[2, 2, 2, 3, 3, 4, 4],
                kernel_size=16, stride=2, groups_width=16,
                verbose=False, use_bn=False, use_do=False, n_classes=150,
            )
            checkpoint = torch.load(self.checkpoint_path, map_location="cpu", weights_only=False)
            model.load_state_dict(checkpoint["state_dict"], strict=True)
            self.model = model.to(self.device).eval()
            self.load_error = None
        except Exception as exc:
            self.model = None
            self.load_error = f"{type(exc).__name__}: {exc}"

    def _preprocess(self, ecg: CommonEcgInput) -> tuple[torch.Tensor, InputSummary]:
        waveform = np.asarray(ecg.waveform, dtype=np.float64)
        if not np.isfinite(waveform).all():
            raise ValueError("waveform must contain only finite values")
        if waveform.ndim != 2 or waveform.shape[0] != 12:
            raise ValueError("ECGFounder requires 12 leads")
        duration = waveform.shape[1] / ecg.sampling_rate_hz
        if duration < self.segment_seconds:
            raise ValueError(f"ECGFounder requires at least 10 seconds; received {duration:g} seconds")

        names = [name.strip().lower() for name in ecg.lead_names]
        required = [name.lower() for name in self.required_leads]
        if set(names) != set(required):
            raise ValueError("ECGFounder requires all 12 standard ECG leads")
        indices = [names.index(name) for name in required]
        waveform = waveform[indices].copy()
        units = ecg.units if len(ecg.units) > 1 else ecg.units * 12
        factors = {"mv": 1.0, "uv": 0.001, "v": 1000.0}
        for target, source in enumerate(indices):
            unit = units[source].strip().lower().replace("µ", "u").replace("μ", "u")
            if unit not in factors:
                raise ValueError(f"unsupported amplitude unit '{units[source]}'; expected V, mV, or uV")
            waveform[target] *= factors[unit]

        # Convert the sampling rate while preserving the full recording duration.
        if ecg.sampling_rate_hz != self.sampling_rate_hz:
            ratio = Fraction(self.sampling_rate_hz / ecg.sampling_rate_hz).limit_denominator(10_000)
            waveform = resample_poly(waveform, ratio.numerator, ratio.denominator, axis=1)
        resampled_sample_count = waveform.shape[1]
        segment_count = resampled_sample_count // self.samples_per_segment
        if segment_count == 0:
            raise ValueError("ECGFounder recording is shorter than 10 seconds after resampling")
        waveform = waveform[:, :segment_count * self.samples_per_segment]
        segments = waveform.reshape(12, segment_count, self.samples_per_segment)
        segments = segments.transpose(1, 0, 2).copy()

        # Each window follows the original single-record global normalization:
        # one mean/std across its 12 leads and 5000 samples.
        mean = segments.mean(axis=(1, 2), keepdims=True)
        std = segments.std(axis=(1, 2), keepdims=True)
        if not np.isfinite(std).all() or not np.isfinite(mean).all() or (std == 0).any():
            raise ValueError("ECGFounder requires every complete window to be nonconstant with finite statistics")
        segments = (segments - mean) / (std + 1e-8)
        return torch.from_numpy(segments.astype(np.float32)), InputSummary(
            lead_count=12,
            sample_count=len(ecg.waveform[0]),
            sampling_rate_hz=ecg.sampling_rate_hz,
            duration_seconds=duration,
            resampled_sample_count=resampled_sample_count,
            segments_processed=segment_count,
        )

    def predict(self, ecg: CommonEcgInput) -> InferenceResponse:
        if not self.ready:
            raise RuntimeError(self.load_error or "ECGFounder is not ready")
        inputs, summary = self._preprocess(ecg)
        with torch.inference_mode():
            logits = self.model(inputs.to(self.device))
            if tuple(logits.shape) != (summary.segments_processed, 150):
                raise RuntimeError(f"Unexpected ECGFounder output shape: {tuple(logits.shape)}")
            window_scores = torch.sigmoid(logits).cpu().numpy()
        if not np.isfinite(window_scores).all():
            raise RuntimeError("ECGFounder returned non-finite scores")
        # Application-level aggregation policy, not a checkpoint-provided rule.
        scores = window_scores.mean(axis=0)
        predictions = [Prediction(label=label, score=float(score), aggregation="mean")
                       for label, score in zip(self.labels, scores)]
        predictions.sort(key=lambda prediction: prediction.score, reverse=True)
        warnings = [
            "Scores are experimental model outputs, not clinical diagnoses.",
            "Scores have not been calibrated as patient-level disease probabilities.",
            "Report-derived labels about changes over time are not verified comparisons with a previous ECG.",
        ]
        dropped_samples = summary.resampled_sample_count - summary.segments_processed * self.samples_per_segment
        if dropped_samples:
            warnings.append(f"Dropped {dropped_samples / self.sampling_rate_hz:g} trailing seconds outside complete 10-second windows.")
        return InferenceResponse(
            model=ModelInfo(id=self.model_id, checkpoint=self.checkpoint_path.name, device=str(self.device)),
            record_id=ecg.record_id,
            input=summary,
            predictions=predictions,
            warnings=warnings,
        )
