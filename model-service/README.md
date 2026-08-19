# CardiSense ECG Model Service

Internal FastAPI service that loads ECG-FM once and accepts a de-identified
Common ECG representation from the Spring Boot backend.

## Requirements

- Python 3.10
- The existing ECG-FM/fairseq-signals dependencies
- `pip==24.0` for the pinned OmegaConf metadata

## Install the API dependencies in the existing environment

```powershell
cd model-service
.\.venv\Scripts\python.exe -m pip install "pip==24.0"
.\.venv\Scripts\python.exe -m pip install "fastapi>=0.116,<1.0" "pydantic>=2.11,<3.0" "uvicorn[standard]>=0.35,<1.0"
```

For a new environment, install the complete dependency file after applying the
Windows fairseq setup workarounds documented during initial setup:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

## Run

```powershell
cd model-service
.\.venv\Scripts\python.exe main.py
```

The service listens on `http://127.0.0.1:8001`. On first startup it downloads
`mimic_iv_ecg_finetuned.pt` into `model-service/ckpts` if the checkpoint is not
already present.

Interactive API documentation:

- `http://127.0.0.1:8001/docs`
- `http://127.0.0.1:8001/redoc`

## Endpoints

- `GET /health` - readiness, device, and checkpoint status
- `GET /models` - available model metadata and input requirements
- `POST /v1/infer` - run ECG-FM on a Common ECG payload

The inference endpoint expects waveform orientation `leads x samples`. It
rejects patient metadata and reference diagnoses because the model service only
accepts the de-identified signal contract.

Minimal request shape:

```json
{
  "model_id": "ecg-fm",
  "ecg": {
    "record_id": "anonymized-uuid",
    "waveform": [[0.0]],
    "lead_names": ["I"],
    "sampling_rate_hz": 500,
    "units": ["mV"]
  }
}
```

The example only illustrates the JSON structure. A valid ECG-FM request must
contain all 12 required leads and at least five seconds of samples.

## Optional configuration

- `ECG_FM_CHECKPOINT`: exact path to an existing checkpoint
- `ECG_FM_CHECKPOINT_DIR`: download directory; defaults to `model-service/ckpts`
- `MODEL_SERVICE_HOST`: defaults to `127.0.0.1`
- `MODEL_SERVICE_PORT`: defaults to `8001`
- `LOG_LEVEL`: defaults to `INFO`
- `ECG_FM_SKIP_MODEL_LOAD=1`: starts the API as not-ready for schema/docs checks

The service is for research prototyping. Its scores are not clinical diagnoses
or calibrated patient-level disease probabilities.
