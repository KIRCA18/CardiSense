import os

import uvicorn

from app.main import app


if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=os.getenv("MODEL_SERVICE_HOST", "127.0.0.1"),
        port=int(os.getenv("MODEL_SERVICE_PORT", "8001")),
    )
