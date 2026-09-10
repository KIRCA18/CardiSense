import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Response

from app.model_registry import adapters, inference_lock, load_models
from app.schemas import AvailableModel, HealthResponse, InferenceRequest, InferenceResponse


logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_: FastAPI):
    load_models()
    yield


app = FastAPI(
    title="CardiSense ECG Model Service",
    version="0.2.0",
    description="Internal inference service for ECG foundation models.",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
def health(response: Response) -> HealthResponse:
    ready_count = sum(adapter.ready for adapter in adapters.values())
    state = "ready" if ready_count == len(adapters) else "degraded" if ready_count else "not_ready"
    if not ready_count:
        response.status_code = 503
    return HealthResponse(status=state, models=[adapter.info() for adapter in adapters.values()])


@app.get("/models", response_model=list[AvailableModel])
def models() -> list[AvailableModel]:
    return [adapter.info() for adapter in adapters.values()]


@app.post("/v1/infer", response_model=InferenceResponse)
def infer(request: InferenceRequest) -> InferenceResponse:
    adapter = adapters.get(request.model_id)
    if adapter is None:
        raise HTTPException(status_code=400, detail=f"Unknown model: {request.model_id}")
    if not adapter.ready:
        raise HTTPException(status_code=503, detail=adapter.load_error or "Model is not ready")
    try:
        with inference_lock:
            return adapter.predict(request.ecg)
    except ValueError as exc:
        raise HTTPException(
            status_code=422,
            detail=str(exc),
        ) from exc
    except RuntimeError as exc:
        logger.exception("Inference failed for %s", request.model_id)
        raise HTTPException(status_code=503, detail=f"Inference failed for {request.model_id}; check model-service logs") from exc
