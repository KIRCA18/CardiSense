import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { CaretDown, Check, Cpu, Pulse, WarningCircle, XCircle } from "@phosphor-icons/react";
import PageHeader from "../components/layout/PageHeader";
import Card from "../components/ui/Card";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import Checkbox from "../components/ui/Checkbox";
import ScoreBar from "../components/ui/ScoreBar";
import EmptyState from "../components/ui/EmptyState";
import EcgMark from "../components/ui/EcgMark";
import * as api from "../lib/mockApi";

export default function AnalysisPage() {
  const [datasets, setDatasets] = useState([]);
  const [models, setModels] = useState([]);
  const [selectedRecordIds, setSelectedRecordIds] = useState(new Set());
  const [selectedModelKeys, setSelectedModelKeys] = useState(new Set());
  const [expanded, setExpanded] = useState(new Set());
  const [running, setRunning] = useState(false);
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api.listDatasetsWithRecords(), api.listModels()]).then(([d, m]) => {
      setDatasets(d);
      setModels(m);
      setLoading(false);
    });
  }, []);

  function toggleRecord(id) {
    setSelectedRecordIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  function toggleDataset(dataset) {
    const allSelected = dataset.records.every((r) => selectedRecordIds.has(r.id));
    setSelectedRecordIds((prev) => {
      const next = new Set(prev);
      dataset.records.forEach((r) => (allSelected ? next.delete(r.id) : next.add(r.id)));
      return next;
    });
  }

  function toggleExpanded(id) {
    setExpanded((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  function toggleModel(key) {
    setSelectedModelKeys((prev) => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  async function handleRun() {
    setRunning(true);
    setResults(null);
    try {
      const runResults = await api.runAnalysis({
        recordIds: [...selectedRecordIds],
        modelKeys: [...selectedModelKeys],
      });
      setResults(runResults);
    } finally {
      setRunning(false);
    }
  }

  const totalRuns = selectedRecordIds.size * selectedModelKeys.size;

  const groupedResults = useMemo(() => {
    if (!results) return [];
    const groups = new Map();
    for (const entry of results) {
      const key = `${entry.datasetName}::${entry.recordFilename}`;
      if (!groups.has(key)) {
        groups.set(key, { datasetName: entry.datasetName, recordFilename: entry.recordFilename, entries: [] });
      }
      groups.get(key).entries.push(entry);
    }
    return [...groups.values()];
  }, [results]);

  return (
    <div className="animate-fade-up pb-24">
      <PageHeader
        title="Analysis"
        description="Select one or more records, choose the models to run, and send the request to the inference server."
      />

      <div className="space-y-6">
        <Card className="p-6">
          <h2 className="mb-1 text-[15px] font-semibold text-ink">1. Select records</h2>
          <p className="mb-4 text-sm text-ink-muted">Choose individual records or select a whole dataset.</p>

          {loading ? (
            <p className="py-6 text-center text-sm text-ink-faint">Loading datasets…</p>
          ) : datasets.length === 0 ? (
            <EmptyState
              title="No datasets yet"
              description="Upload a folder or ZIP of ECG records to get started."
              action={
                <Button as={Link} to="/app/upload" size="sm">
                  Upload ECGs
                </Button>
              }
            />
          ) : (
            <div className="divide-y divide-border rounded-lg border border-border">
              {datasets.map((dataset) => {
                const allSelected = dataset.records.every((r) => selectedRecordIds.has(r.id));
                const someSelected = dataset.records.some((r) => selectedRecordIds.has(r.id));
                const isExpanded = expanded.has(dataset.id);
                return (
                  <div key={dataset.id}>
                    <div className="flex items-center gap-3 bg-surface-alt/60 px-4 py-2.5">
                      <Checkbox
                        checked={allSelected}
                        onChange={() => toggleDataset(dataset)}
                        aria-label={`Select all records in ${dataset.name}`}
                        className={someSelected && !allSelected ? "border-primary bg-primary-soft" : ""}
                      />
                      <button
                        type="button"
                        aria-expanded={isExpanded}
                        onClick={() => toggleExpanded(dataset.id)}
                        className="group flex flex-1 items-center justify-between gap-2 rounded-md py-1 text-left"
                      >
                        <span className="text-sm font-medium text-ink">
                          {dataset.name}{" "}
                          <span className="font-normal text-ink-faint">· {dataset.records.length} records</span>
                        </span>
                        <span className="flex items-center gap-1.5 text-xs font-medium text-ink-faint transition-colors group-hover:text-primary">
                          {isExpanded ? "Hide records" : "Show records"}
                          <CaretDown
                            size={14}
                            weight="bold"
                            className={`transition-transform ${isExpanded ? "rotate-180" : ""}`}
                          />
                        </span>
                      </button>
                    </div>
                    {isExpanded && (
                      <ul className="divide-y divide-border">
                        {dataset.records.map((record) => (
                          <li key={record.id}>
                            <button
                              type="button"
                              onClick={() => toggleRecord(record.id)}
                              aria-pressed={selectedRecordIds.has(record.id)}
                              className="flex w-full flex-col gap-1 px-4 py-2 pl-11 text-left hover:bg-surface-alt/50 sm:flex-row sm:items-center sm:gap-3"
                            >
                              <div className="flex min-w-0 items-center gap-3">
                                <span
                                  aria-hidden="true"
                                  className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-colors ${
                                    selectedRecordIds.has(record.id)
                                      ? "border-primary bg-primary text-white"
                                      : "border-border-strong bg-canvas text-transparent"
                                  }`}
                                >
                                  <Check size={13} weight="bold" />
                                </span>
                                <span className="min-w-0 flex-1 truncate font-mono text-xs text-ink-muted">{record.filename}</span>
                              </div>
                              <span className="pl-8 text-xs text-ink-faint sm:shrink-0 sm:pl-0">
                                {record.leadCount} lead{record.leadCount > 1 ? "s" : ""} · {record.samplingRateHz} Hz ·{" "}
                                {record.durationSeconds}s
                              </span>
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="mb-1 text-[15px] font-semibold text-ink">2. Select models</h2>
          <p className="mb-4 text-sm text-ink-muted">Run one model or compare several against the same records.</p>

          <div className="grid gap-3 sm:grid-cols-3">
            {models.map((model) => {
              const isSelected = selectedModelKeys.has(model.key);
              return (
                <button
                  key={model.key}
                  type="button"
                  aria-pressed={isSelected}
                  onClick={() => toggleModel(model.key)}
                  className={`flex flex-col items-start gap-2 rounded-xl border p-4 text-left transition-colors ${
                    isSelected ? "border-primary bg-primary-soft" : "border-border hover:border-border-strong"
                  }`}
                >
                  <div className="flex w-full items-center justify-between">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-canvas text-primary-strong">
                      <Cpu size={16} />
                    </div>
                    <span
                      aria-hidden="true"
                      className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-colors ${
                        isSelected ? "border-primary bg-primary text-white" : "border-border-strong bg-canvas text-transparent"
                      }`}
                    >
                      <Check size={13} weight="bold" />
                    </span>
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-ink">
                      {model.name} <span className="font-normal text-ink-faint">{model.version}</span>
                    </p>
                    <p className="mt-0.5 text-xs leading-relaxed text-ink-muted">{model.description}</p>
                  </div>
                  <Badge tone="neutral">{model.device}</Badge>
                </button>
              );
            })}
          </div>
        </Card>

        <Card className="flex items-center justify-between p-6">
          <div>
            <p className="text-sm font-medium text-ink">
              {selectedRecordIds.size} record{selectedRecordIds.size === 1 ? "" : "s"} ×{" "}
              {selectedModelKeys.size} model{selectedModelKeys.size === 1 ? "" : "s"}
            </p>
            <p className="text-sm text-ink-muted">
              {totalRuns > 0 ? `${totalRuns} analyses will be run` : "Select at least one record and one model"}
            </p>
          </div>
          <Button onClick={handleRun} disabled={totalRuns === 0 || running} size="lg">
            {running ? "Running…" : (
              <>
                <Pulse size={16} /> Run analysis
              </>
            )}
          </Button>
        </Card>

        {running && (
          <Card className="flex flex-col items-center justify-center gap-4 p-12">
            <EcgMark animated className="h-10 w-24 text-primary" strokeWidth={3.5} />
            <p className="text-sm text-ink-muted">Sending {totalRuns} request{totalRuns === 1 ? "" : "s"} to the inference server…</p>
          </Card>
        )}

        {results && (
          <div className="space-y-4">
            <h2 className="text-[15px] font-semibold text-ink">Results</h2>
            {groupedResults.map((group) => (
              <Card key={`${group.datasetName}-${group.recordFilename}`} className="p-6">
                <div className="mb-4">
                  <p className="font-mono text-sm font-medium text-ink">{group.recordFilename}</p>
                  <p className="text-xs text-ink-faint">{group.datasetName}</p>
                </div>
                <div className="space-y-5">
                  {group.entries.map((entry) => (
                    <div key={entry.id} className="border-t border-border pt-4 first:border-t-0 first:pt-0">
                      <div className="mb-3 flex items-center justify-between">
                        <span className="text-sm font-semibold text-ink">{entry.modelName}</span>
                        <Badge tone={entry.status === "completed" ? "success" : "danger"}>{entry.status}</Badge>
                      </div>

                      {entry.status === "failed" ? (
                        <div className="flex items-start gap-2 rounded-lg bg-danger-soft p-3 text-sm text-danger">
                          <XCircle size={16} className="mt-0.5 shrink-0" />
                          {entry.errorMessage}
                        </div>
                      ) : (
                        <>
                          <div className="space-y-2">
                            {entry.predictions.map((p, i) => (
                              <ScoreBar key={p.label} label={p.label} score={p.score} rank={i + 1} />
                            ))}
                          </div>
                          {entry.warnings.length > 0 && (
                            <div className="mt-3 flex items-start gap-2 rounded-lg bg-warning-soft p-3 text-sm text-warning">
                              <WarningCircle size={16} className="mt-0.5 shrink-0" />
                              <ul>
                                {entry.warnings.map((w) => (
                                  <li key={w}>{w}</li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  ))}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
