FROM python:3.10-slim

ENV PIP_NO_CACHE_DIR=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# CPU build of PyTorch: the +cu118 wheels pinned in requirements.txt need an NVIDIA GPU.
COPY requirements.txt .
RUN grep -v '^-e ' requirements.txt | grep -v '^--extra-index-url' | sed 's/+cu118/+cpu/' > requirements-base.txt \
    && pip install --extra-index-url https://download.pytorch.org/whl/cpu -r requirements-base.txt wheel

# fairseq and fairseq-signals must be compiled against the PyTorch installed above,
# so they are built without pip's isolated build environment.
RUN git clone https://github.com/facebookresearch/fairseq.git /opt/fairseq \
    && git -C /opt/fairseq checkout 3d262bb25690e4eb2e7d3c1309b1e9c406ca4b99 \
    && git clone https://github.com/Jwoo5/fairseq-signals.git /opt/fairseq-signals \
    && git -C /opt/fairseq-signals checkout f8f0ff1c788a82c2059cb452cd5462898867489e \
    && pip install --no-build-isolation --no-deps -e /opt/fairseq-signals \
    && pip install --no-build-isolation -e /opt/fairseq

RUN git clone --depth 1 https://github.com/PKUDigitalHealth/ECGFounder.git /app/vendor/ECGFounder

COPY main.py .
COPY app ./app

ENV MODEL_SERVICE_HOST=0.0.0.0 \
    MODEL_SERVICE_PORT=8001

EXPOSE 8001

# ECG-FM downloads its weights on first start; ECGFounder's weights are fetched here if missing.
CMD ["sh", "-c", "test -f ckpts/12_lead_ECGFounder.pth || python -c \"from huggingface_hub import hf_hub_download; hf_hub_download('PKUDigitalHealth/ECGFounder', '12_lead_ECGFounder.pth', local_dir='ckpts')\"; exec python main.py"]
