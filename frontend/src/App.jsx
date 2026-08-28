import { useState } from "react";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8000";

function App() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();

    if (!file) {
      setError("Choose a ZIP file first.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("modelId", "ecg-fm");

    setIsLoading(true);
    setError("");
    setResult(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/analyses`, {
        method: "POST",
        credentials: "include",
        body: formData,
      });
      const body = await response.json().catch(() => null);

      if (!response.ok) {
        throw new Error(
          body?.detail ?? body?.message ?? "The ECG could not be analyzed.",
        );
      }

      setResult(body);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not connect to the backend.",
      );
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="card">
        <header>
          <p className="eyebrow">CardiSense</p>
          <h1>ECG analysis</h1>
          <p className="intro">
            Upload one flat archive containing multiple WFDB .hea and .mat
            files and analyze every record with ECG-FM.
          </p>
        </header>

        <form onSubmit={handleSubmit}>
          <label htmlFor="ecg-file">ECG dataset archive</label>
          <input
            id="ecg-file"
            type="file"
            accept=".zip,.rar,.7z,.tar,.tar.gz,.tgz,application/zip,application/vnd.rar,application/x-7z-compressed,application/x-tar,application/gzip"
            disabled={isLoading}
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setError("");
            }}
          />

          <button type="submit" disabled={!file || isLoading}>
            {isLoading ? "Analyzing…" : "Analyze dataset"}
          </button>
        </form>

        {isLoading && (
          <div className="status" role="status">
            <span className="spinner" aria-hidden="true" />
            The model is processing each ECG in the dataset. This may take a moment.
          </div>
        )}

        {error && <div className="error" role="alert">{error}</div>}

        {result && <DatasetAnalysisResult result={result} />}
      </section>
    </main>
  );
}

function DatasetAnalysisResult({ result }) {
  const records = result.records ?? [];

  return (
    <section className="results" aria-live="polite">
      <div className="results-heading">
        <div>
          <p className="eyebrow">Dataset response</p>
          <h2>Analysis complete</h2>
        </div>
        <span className="model-badge">
          {result.recordCount ?? records.length} records
        </span>
      </div>

      <div className="record-results">
        {records.map((record) => (
          <AnalysisResult
            key={record.recordName}
            recordName={record.recordName}
            result={record.analysis}
          />
        ))}
      </div>
    </section>
  );
}

function AnalysisResult({ recordName, result }) {
  return (
    <article className="record-result">
      <div className="results-heading">
        <div>
          <p className="eyebrow">WFDB record</p>
          <h3 className="record-name">{recordName}</h3>
        </div>
        <span className="model-badge">{result.model?.id ?? "ECG-FM"}</span>
      </div>

      <dl className="summary">
        <div>
          <dt>Record</dt>
          <dd>{result.recordId}</dd>
        </div>
        <div>
          <dt>Leads</dt>
          <dd>{result.input?.leadCount}</dd>
        </div>
        <div>
          <dt>Duration</dt>
          <dd>{formatNumber(result.input?.durationSeconds)} s</dd>
        </div>
        <div>
          <dt>Segments</dt>
          <dd>{result.input?.segmentsProcessed}</dd>
        </div>
      </dl>

      <h4>Diagnostic probabilities</h4>
      <div className="predictions">
        {result.predictions?.map((prediction) => (
          <div className="prediction" key={prediction.label}>
            <span>{prediction.label}</span>
            <strong>{(prediction.score * 100).toFixed(2)}%</strong>
          </div>
        ))}
      </div>

      {result.warnings?.length > 0 && (
        <div className="warnings">
          <strong>Warnings</strong>
          <ul>
            {result.warnings.map((warning) => <li key={warning}>{warning}</li>)}
          </ul>
        </div>
      )}
    </article>
  );
}

function formatNumber(value) {
  return typeof value === "number" ? value.toFixed(1) : "—";
}

export default App;
