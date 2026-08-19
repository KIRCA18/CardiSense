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
            Upload a ZIP containing a WFDB ECG record and analyze it with ECG-FM.
          </p>
        </header>

        <form onSubmit={handleSubmit}>
          <label htmlFor="ecg-file">ECG ZIP file</label>
          <input
            id="ecg-file"
            type="file"
            accept=".zip,application/zip"
            disabled={isLoading}
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null);
              setError("");
            }}
          />

          <button type="submit" disabled={!file || isLoading}>
            {isLoading ? "Analyzing…" : "Analyze ECG"}
          </button>
        </form>

        {isLoading && (
          <div className="status" role="status">
            <span className="spinner" aria-hidden="true" />
            The model is processing the ECG. This may take a moment.
          </div>
        )}

        {error && <div className="error" role="alert">{error}</div>}

        {result && <AnalysisResult result={result} />}
      </section>
    </main>
  );
}

function AnalysisResult({ result }) {
  return (
    <section className="results" aria-live="polite">
      <div className="results-heading">
        <div>
          <p className="eyebrow">Model response</p>
          <h2>Analysis complete</h2>
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

      <h3>Diagnostic probabilities</h3>
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
    </section>
  );
}

function formatNumber(value) {
  return typeof value === "number" ? value.toFixed(1) : "—";
}

export default App;
