import os
import numpy as np
import torch
import wfdb
from scipy.signal import resample_poly
from huggingface_hub import hf_hub_download

from fairseq_signals.models import build_model_from_checkpoint

# ==========================================
# Constants verified from the official repo
# ==========================================
LEAD_ORDER = ['I', 'II', 'III', 'aVR', 'aVL', 'aVF',
              'V1', 'V2', 'V3', 'V4', 'V5', 'V6']
SAMPLE_RATE = 500          # Hz, required by this checkpoint
SEGMENT_SEC = 5
N_SAMPLES = SAMPLE_RATE * SEGMENT_SEC   # 2500

# Confirmed order (data/mimic_iv_ecg/labels/label_def.csv), cross-checked
# against the pos_weight vector embedded in mimic_iv_ecg_finetuned.yaml.
LABEL_NAMES = [
    "Poor data quality",
    "Sinus rhythm",
    "Premature ventricular contraction",
    "Tachycardia",
    "Ventricular tachycardia",
    "Supraventricular tachycardia with aberrancy",
    "Atrial fibrillation",
    "Atrial flutter",
    "Bradycardia",
    "Accessory pathway conduction",
    "Atrioventricular block",
    "1st degree atrioventricular block",
    "Bifascicular block",
    "Right bundle branch block",
    "Left bundle branch block",
    "Infarction",
    "Electronic pacemaker",
]

# Some labels are noisy/rare per-segment, so the official pipeline aggregates
# multiple 5s segments per recording with either 'mean' or 'max'.
AGG_METHODS = {
    'Poor data quality': 'max',
    'Sinus rhythm': 'mean',
    'Premature ventricular contraction': 'max',
    'Tachycardia': 'mean',
    'Ventricular tachycardia': 'max',
    'Supraventricular tachycardia with aberrancy': 'max',
    'Bradycardia': 'mean',
    'Infarction': 'mean',
    'Atrioventricular block': 'mean',
    'Right bundle branch block': 'mean',
    'Left bundle branch block': 'mean',
    'Electronic pacemaker': 'max',
    'Atrial fibrillation': 'mean',
    'Atrial flutter': 'mean',
    'Accessory pathway conduction': 'mean',
    '1st degree atrioventricular block': 'mean',
    'Bifascicular block': 'mean',
}


# ==========================================
# 1. Download the FINE-TUNED checkpoint
# ==========================================
def download_finetuned_checkpoint(local_dir="ckpts"):
    os.makedirs(local_dir, exist_ok=True)
    ckpt_path = hf_hub_download(
        repo_id="wanglab/ecg-fm",
        filename="mimic_iv_ecg_finetuned.pt",
        local_dir=local_dir,
    )
    # yaml isn't strictly required for build_model_from_checkpoint, but the
    # repo ships it alongside the weights for reproducibility
    hf_hub_download(
        repo_id="wanglab/ecg-fm",
        filename="mimic_iv_ecg_finetuned.yaml",
        local_dir=local_dir,
    )
    return ckpt_path


# ==========================================
# 2. Ingestion + preprocessing to match the
#    checkpoint's exact training-time recipe
# ==========================================
def load_ptbxl_ecg_500hz(record="00001_hr", pn_dir="ptb-xl/1.0.3/records500/00000/"):
    """
    NOTE: use records500 (the "_hr" high-resolution PTB-XL files), not
    records100 -- this checkpoint was trained at 500 Hz. Using the 100 Hz
    files would silently feed the model a signal 5x slower than it expects.
    """
    signals, fields = wfdb.rdsamp(record, pn_dir=pn_dir)
    sig_names = fields['sig_name']
    fs = fields['fs']
    return signals, sig_names, fs


def preprocess(signals, sig_names, fs):
    """
    signals: (time, leads) raw array from wfdb
    Returns: (n_segments, 12, N_SAMPLES) float32 tensor ready for the model
    """
    # 1. Reorder leads to the exact order the model expects.
    # Match case-insensitively since sources vary (e.g. PTB-XL's WFDB
    # headers use 'AVR'/'AVL'/'AVF' rather than 'aVR'/'aVL'/'aVF').
    sig_names_lower = [s.lower() for s in sig_names]
    missing = [l for l in LEAD_ORDER if l.lower() not in sig_names_lower]
    if missing:
        raise ValueError(
            f"Recording is missing required leads: {missing}. "
            f"Available leads: {sig_names}"
        )
    lead_idx = [sig_names_lower.index(l.lower()) for l in LEAD_ORDER]
    signals = signals[:, lead_idx]  # (time, 12)

    # 2. Resample to 500 Hz if needed
    if fs != SAMPLE_RATE:
        signals = resample_poly(signals, SAMPLE_RATE, fs, axis=0)

    # 3. Per-lead z-score standardization (matches ecg_transform.t.scale.Standardize)
    mean = signals.mean(axis=0, keepdims=True)
    std = signals.std(axis=0, keepdims=True)
    std[std == 0] = 1.0
    signals = (signals - mean) / std

    # 4. Segment into non-overlapping 5s (2500-sample) windows, drop remainder
    n_total = signals.shape[0]
    n_segments = n_total // N_SAMPLES
    if n_segments == 0:
        raise ValueError("Recording shorter than one 5s segment at 500 Hz.")
    signals = signals[: n_segments * N_SAMPLES]
    segments = signals.reshape(n_segments, N_SAMPLES, 12).transpose(0, 2, 1)  # (n_seg, 12, N_SAMPLES)

    return torch.tensor(segments, dtype=torch.float32)


# ==========================================
# 3. Inference with the fine-tuned model
# ==========================================
def run_inference(model, segments, device):
    model.eval()
    with torch.no_grad():
        out = model(source=segments.to(device))
        logits = out['out'] if isinstance(out, dict) else out[0]
        # Multi-label task -> sigmoid per class, NOT softmax
        probs = torch.sigmoid(logits).cpu().numpy()  # (n_segments, 17)
    return probs


def aggregate_predictions(probs):
    """Aggregate per-segment probabilities into one prediction per recording,
    using each label's official aggregation method (mean or max)."""
    agg = {}
    for i, name in enumerate(LABEL_NAMES):
        col = probs[:, i]
        agg[name] = float(col.max() if AGG_METHODS[name] == 'max' else col.mean())
    return agg


# ==========================================
# 4. Main
# ==========================================
def main():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"--- SYSTEM INIT ---\nHardware target: {device}")

    print("Downloading fine-tuned ECG-FM checkpoint (mimic_iv_ecg_finetuned.pt)...")
    ckpt_path = download_finetuned_checkpoint()

    print("Downloading a real 500 Hz clinical ECG from PTB-XL...")
    signals, sig_names, fs = load_ptbxl_ecg_500hz()
    print(f"Raw signal: {signals.shape[0]} samples x {signals.shape[1]} leads at {fs} Hz")

    segments = preprocess(signals, sig_names, fs)
    print(f"Preprocessed into {segments.shape[0]} segment(s) of shape {tuple(segments.shape[1:])}")

    print("Loading the fine-tuned 17-label diagnosis model...")
    model = build_model_from_checkpoint(checkpoint_path=ckpt_path)
    model = model.to(device)

    print("Running inference...")
    probs = run_inference(model, segments, device)
    results = aggregate_predictions(probs)

    print("\n==========================================")
    print("       ECG-FM DIAGNOSTIC PROBABILITIES")
    print("   (multi-label: several can be positive)")
    print("==========================================")
    for name, p in sorted(results.items(), key=lambda x: -x[1]):
        print(f"{name:<45}: {p*100:>6.2f}%")
    print("==========================================")


if __name__ == "__main__":
    main()
