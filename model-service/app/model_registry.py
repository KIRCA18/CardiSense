import logging
import os
import threading

from app.adapters.base import ModelAdapter
from app.adapters.ecg_fm import EcgFmAdapter
from app.adapters.ecg_founder import EcgFounderAdapter


logger = logging.getLogger(__name__)
adapters: dict[str, ModelAdapter] = {
    "ecg-fm": EcgFmAdapter(),
    "ecg-founder": EcgFounderAdapter(),
}

inference_lock = threading.Lock()


def load_models() -> None:
    for model_id, adapter in adapters.items():
        skip_variable = model_id.upper().replace("-", "_") + "_SKIP_MODEL_LOAD"
        if os.getenv(skip_variable) == "1":
            adapter.load_error = f"Model loading skipped by {skip_variable}=1"
            continue
        try:
            adapter.load()
        except Exception as exc:
            adapter.load_error = f"{type(exc).__name__}: {exc}"
        if adapter.ready:
            logger.info("%s loaded", model_id)
        else:
            logger.error("%s unavailable: %s", model_id, adapter.load_error)
