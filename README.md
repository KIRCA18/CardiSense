# Setup and Run Instructions

## Requirements

- Python 3.10 - required for compatibility with the pinned AI/ML dependencies (PyTorch, fairseq, etc.), several of which do not yet support newer Python versions.

## 1. Create a virtual environment

**Windows**

```bash
py -3.10 -m venv venv
```

**Linux / macOS**

```bash
python3.10 -m venv venv
```

## 2. Activate the virtual environment

**Windows**

```bash
venv\Scripts\activate
```

**Linux / macOS**

```bash
source venv/bin/activate
```

## 3. Install dependencies

```bash
pip install -r requirements.txt
```


## 4. Run the project

```bash
python main.py
```