import os

import uvicorn

from app.main import app

# Activate venv: .venv\Scripts\Activate.ps1
# Run service: python main.py

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=os.getenv("MODEL_SERVICE_HOST", "127.0.0.1"),
        port=int(os.getenv("MODEL_SERVICE_PORT", "8001")),
    )
