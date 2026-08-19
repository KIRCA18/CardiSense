import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Response, status

from app.ecg_fm import LEAD_ORDER, MODEL_ID, SAMPLE_RATE, SEGMENT_SECONDS, EcgFmModel
from app.schemas import AvailableModel, HealthResponse, InferenceRequest, InferenceResponse


logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)
ecg_fm = EcgFmModel()


@asynccontextmanager
async def lifespan(_: FastAPI):
    if os.getenv("ECG_FM_SKIP_MODEL_LOAD") != "1":
        logger.info("Loading ECG-FM on %s", ecg_fm.device)
        ecg_fm.load()
        if ecg_fm.ready:
            logger.info("ECG-FM loaded from %s", ecg_fm.checkpoint_path)
        else:
            logger.error("ECG-FM failed to load: %s", ecg_fm.load_error)
    else:
        ecg_fm.load_error = "Model loading skipped by ECG_FM_SKIP_MODEL_LOAD=1"
    yield


app = FastAPI(
    title="CardiSense ECG Model Service",
    version="0.1.0",
    description="Internal inference service for ECG foundation models.",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
def health(response: Response) -> HealthResponse:
    if not ecg_fm.ready:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    return HealthResponse(
        status="ready" if ecg_fm.ready else "not_ready",
        model_id=MODEL_ID,
        device=str(ecg_fm.device),
        checkpoint_loaded=ecg_fm.ready,
        detail=None if ecg_fm.ready else ecg_fm.load_error,
    )


@app.get("/models", response_model=list[AvailableModel])
def models() -> list[AvailableModel]:
    return [AvailableModel(
        id=MODEL_ID,
        name="ECG-FM",
        status="available" if ecg_fm.ready else "unavailable",
        required_leads=LEAD_ORDER,
        sampling_rate_hz=SAMPLE_RATE,
        segment_seconds=SEGMENT_SECONDS,
    )]


@app.post("/v1/infer", response_model=InferenceResponse)
def infer(request: InferenceRequest) -> InferenceResponse:
    if not ecg_fm.ready:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=ecg_fm.load_error or "ECG-FM is not ready",
        )
    try:
        return ecg_fm.predict(request.ecg)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        ) from exc
