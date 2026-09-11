import os
import threading
from fractions import Fraction
from pathlib import Path

import numpy as np
import torch
from huggingface_hub import hf_hub_download
from scipy.signal import resample_poly

from app.adapters.base import ModelAdapter

from app.schemas import CommonEcgInput, InferenceResponse, InputSummary, ModelInfo, Prediction


MODEL_ID = "ecg-fm"
CHECKPOINT_FILENAME = "mimic_iv_ecg_finetuned.pt"
LEAD_ORDER = ["I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"]
SAMPLE_RATE = 500
SEGMENT_SECONDS = 5
SAMPLES_PER_SEGMENT = SAMPLE_RATE * SEGMENT_SECONDS

LABEL_NAMES = [
    "Poor data quality", "Sinus rhythm", "Premature ventricular contraction",
    "Tachycardia", "Ventricular tachycardia",
    "Supraventricular tachycardia with aberrancy", "Atrial fibrillation",
    "Atrial flutter", "Bradycardia", "Accessory pathway conduction",
    "Atrioventricular block", "1st degree atrioventricular block",
    "Bifascicular block", "Right bundle branch block", "Left bundle branch block",
    "Infarction", "Electronic pacemaker",
]

AGGREGATION_METHODS = {
    "Poor data quality": "max", "Sinus rhythm": "mean",
    "Premature ventricular contraction": "max", "Tachycardia": "mean",
    "Ventricular tachycardia": "max",
    "Supraventricular tachycardia with aberrancy": "max",
    "Atrial fibrillation": "mean", "Atrial flutter": "mean",
    "Bradycardia": "mean", "Accessory pathway conduction": "mean",
    "Atrioventricular block": "mean", "1st degree atrioventricular block": "mean",
    "Bifascicular block": "mean", "Right bundle branch block": "mean",
    "Left bundle branch block": "mean", "Infarction": "mean",
    "Electronic pacemaker": "max",
}

UNIT_TO_MILLIVOLTS = {"mv": 1.0, "uv": 0.001, "v": 1000.0}


class EcgFmAdapter(ModelAdapter):
    model_id = MODEL_ID
    name = "ECG-FM"
    required_leads = LEAD_ORDER
    sampling_rate_hz = SAMPLE_RATE
    segment_seconds = SEGMENT_SECONDS
    accepts_other_sampling_rates = True

    def __init__(self) -> None:
        self.device = torch.device(os.getenv("ECG_FM_DEVICE", "cuda" if torch.cuda.is_available() else "cpu"))
        self.model = None
        self.checkpoint_path: Path | None = None
        self.load_error: str | None = None
        self._inference_lock = threading.Lock()

    @property
    def ready(self) -> bool:
        return self.model is not None and self.load_error is None

    def load(self) -> None:
        try:
            from fairseq_signals.models import build_model_from_checkpoint

            configured_checkpoint = os.getenv("ECG_FM_CHECKPOINT")
            if configured_checkpoint:
                checkpoint_path = Path(configured_checkpoint).expanduser().resolve()
                if not checkpoint_path.is_file():
                    raise FileNotFoundError(f"ECG_FM_CHECKPOINT does not exist: {checkpoint_path}")
            else:
                default_dir = Path(__file__).resolve().parents[2] / "ckpts"
                checkpoint_dir = Path(os.getenv("ECG_FM_CHECKPOINT_DIR", str(default_dir))).expanduser()
                checkpoint_dir.mkdir(parents=True, exist_ok=True)
                checkpoint_path = Path(hf_hub_download(
                    repo_id="wanglab/ecg-fm",
                    filename=CHECKPOINT_FILENAME,
                    local_dir=str(checkpoint_dir),
                ))

            model = build_model_from_checkpoint(checkpoint_path=str(checkpoint_path))
            model = model.to(self.device)
            model.eval()
            self.model = model
            self.checkpoint_path = checkpoint_path
            self.load_error = None
        except Exception as exc:
            self.model = None
            self.load_error = f"{type(exc).__name__}: {exc}"

    def predict(self, ecg: CommonEcgInput) -> InferenceResponse:
        if not self.ready:
            raise RuntimeError(self.load_error or "ECG-FM is not loaded")

        segments, summary = self._preprocess(ecg)
        with self._inference_lock, torch.inference_mode():
            output = self.model(source=segments.to(self.device))
            logits = output["out"] if isinstance(output, dict) else output[0]
            probabilities = torch.sigmoid(logits).detach().cpu().numpy()

        if probabilities.ndim != 2 or probabilities.shape[1] != len(LABEL_NAMES):
            raise RuntimeError(
                f"Unexpected ECG-FM output shape: {probabilities.shape}; "
                f"expected (segments, {len(LABEL_NAMES)})"
            )

        predictions = []
        for index, label in enumerate(LABEL_NAMES):
            method = AGGREGATION_METHODS[label]
            segment_scores = probabilities[:, index]
            score = float(segment_scores.max()) if method == "max" else float(segment_scores.mean())
            predictions.append(Prediction(label=label, score=score, aggregation=method))
        predictions.sort(key=lambda prediction: prediction.score, reverse=True)

        return InferenceResponse(
            model=ModelInfo(
                id=MODEL_ID,
                checkpoint=self.checkpoint_path.name,
                device=str(self.device),
            ),
            record_id=ecg.record_id,
            input=summary,
            predictions=predictions,
            warnings=[
                "Scores are experimental model outputs, not clinical diagnoses.",
                "Scores have not been calibrated as patient-level disease probabilities.",
            ],
        )

    def _preprocess(self, ecg: CommonEcgInput) -> tuple[torch.Tensor, InputSummary]:
        waveform = np.asarray(ecg.waveform, dtype=np.float32)
        if waveform.ndim != 2 or not np.isfinite(waveform).all():
            raise ValueError("waveform must be a finite two-dimensional numeric array")

        lead_names_lower = [name.strip().lower() for name in ecg.lead_names]
        missing = [lead for lead in LEAD_ORDER if lead.lower() not in lead_names_lower]
        if missing:
            raise ValueError(
                f"recording is missing required leads: {missing}; available leads: {ecg.lead_names}"
            )

        lead_indices = [lead_names_lower.index(lead.lower()) for lead in LEAD_ORDER]
        waveform = waveform[lead_indices, :]

        units = ecg.units if len(ecg.units) > 1 else ecg.units * len(ecg.lead_names)
        ordered_units = [units[index] for index in lead_indices]
        for lead_index, unit in enumerate(ordered_units):
            normalized_unit = unit.strip().lower().replace("μ", "u").replace("µ", "u")
            if normalized_unit not in UNIT_TO_MILLIVOLTS:
                raise ValueError(f"unsupported amplitude unit '{unit}'; expected V, mV, or uV")
            waveform[lead_index] *= UNIT_TO_MILLIVOLTS[normalized_unit]

        original_sample_count = waveform.shape[1]
        original_duration = original_sample_count / ecg.sampling_rate_hz

        if not np.isclose(ecg.sampling_rate_hz, SAMPLE_RATE):
            ratio = Fraction(SAMPLE_RATE / ecg.sampling_rate_hz).limit_denominator(10_000)
            waveform = resample_poly(waveform, up=ratio.numerator, down=ratio.denominator, axis=1)

        mean = waveform.mean(axis=1, keepdims=True)
        std = waveform.std(axis=1, keepdims=True)
        std[std == 0] = 1.0
        waveform = (waveform - mean) / std

        resampled_sample_count = waveform.shape[1]
        segment_count = resampled_sample_count // SAMPLES_PER_SEGMENT
        if segment_count == 0:
            raise ValueError(f"recording is shorter than {SEGMENT_SECONDS} seconds after resampling")

        waveform = waveform[:, : segment_count * SAMPLES_PER_SEGMENT]
        segments = waveform.reshape(12, segment_count, SAMPLES_PER_SEGMENT)
        segments = segments.transpose(1, 0, 2).copy()

        return torch.from_numpy(segments).to(dtype=torch.float32), InputSummary(
            lead_count=len(LEAD_ORDER),
            sample_count=original_sample_count,
            sampling_rate_hz=ecg.sampling_rate_hz,
            duration_seconds=original_duration,
            resampled_sample_count=resampled_sample_count,
            segments_processed=segment_count,
        )
