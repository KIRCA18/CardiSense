import { useEffect, useState } from "react";
import { CaretDown, MagnifyingGlass, WarningCircle, XCircle } from "@phosphor-icons/react";
import PageHeader from "../components/layout/PageHeader";
import Card from "../components/ui/Card";
import Badge from "../components/ui/Badge";
import ScoreBar from "../components/ui/ScoreBar";
import EmptyState from "../components/ui/EmptyState";
import * as api from "../lib/mockApi";
import { formatDateTime, statusTone } from "../lib/format";

const STATUS_FILTERS = ["all", "completed", "failed"];

export default function HistoryPage() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    api.listHistory().then((h) => {
      setHistory(h);
      setLoading(false);
    });
  }, []);

  const filtered = history.filter((h) => {
    const matchesQuery =
      h.recordFilename.toLowerCase().includes(query.toLowerCase()) ||
      h.datasetName.toLowerCase().includes(query.toLowerCase()) ||
      h.modelName.toLowerCase().includes(query.toLowerCase());
    const matchesStatus = statusFilter === "all" || h.status === statusFilter;
    return matchesQuery && matchesStatus;
  });

  return (
    <div className="animate-fade-up">
      <PageHeader title="History" description="Every analysis you've run, across every dataset and model." />

      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative w-full sm:max-w-sm">
          <MagnifyingGlass size={16} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-faint" />
          <input
            aria-label="Search by record, dataset, or model"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by record, dataset, or model"
            className="h-10 w-full rounded-lg border border-border bg-canvas pl-9 pr-3 text-sm text-ink placeholder:text-ink-faint focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary-soft"
          />
        </div>
        <div className="flex gap-1 rounded-lg bg-surface-alt p-1 self-start">
          {STATUS_FILTERS.map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => setStatusFilter(status)}
              className={`rounded-md px-3 py-1.5 text-xs font-medium capitalize transition-colors ${
                statusFilter === status ? "bg-canvas text-ink shadow-sm" : "text-ink-muted hover:text-ink"
              }`}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <p className="py-10 text-center text-sm text-ink-faint">Loading history…</p>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No analyses found"
          description={history.length === 0 ? "Run your first analysis to see it appear here." : "Try a different search or filter."}
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="hidden gap-3 border-b border-border bg-surface-alt/60 px-5 py-2.5 text-xs font-medium uppercase tracking-wide text-ink-faint md:grid md:grid-cols-[2fr_1.4fr_1fr_1fr_auto]">
            <span>Record</span>
            <span>Model</span>
            <span>Status</span>
            <span>Started</span>
            <span />
          </div>
          <div className="divide-y divide-border">
            {filtered.map((entry) => {
              const isExpanded = expandedId === entry.id;
              return (
                <div key={entry.id}>
                  <button
                    type="button"
                    onClick={() => setExpandedId(isExpanded ? null : entry.id)}
                    className="flex w-full flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3 text-left hover:bg-surface-alt/40 md:flex-nowrap md:gap-3 md:px-5"
                  >
                    <span className="min-w-0 basis-full md:basis-0 md:flex-[2]">
                      <span className="block truncate font-mono text-sm text-ink">{entry.recordFilename}</span>
                      <span className="block truncate text-xs text-ink-faint">{entry.datasetName}</span>
                    </span>
                    <span className="min-w-0 truncate text-sm text-ink-muted md:min-w-0 md:flex-1">{entry.modelName}</span>
                    <Badge tone={statusTone(entry.status)}>{entry.status}</Badge>
                    <span className="text-xs text-ink-faint md:flex-1 md:text-sm">{formatDateTime(entry.startedAt)}</span>
                    <CaretDown
                      size={14}
                      className={`ml-auto shrink-0 text-ink-faint transition-transform md:ml-0 ${isExpanded ? "rotate-180" : ""}`}
                    />
                  </button>

                  {isExpanded && (
                    <div className="border-t border-border bg-surface-alt/30 px-5 py-4">
                      {entry.status === "failed" ? (
                        <div className="flex items-start gap-2 rounded-lg bg-danger-soft p-3 text-sm text-danger">
                          <XCircle size={16} className="mt-0.5 shrink-0" />
                          {entry.errorMessage}
                        </div>
                      ) : (
                        <div className="grid gap-6 lg:grid-cols-[1fr_auto]">
                          <div className="space-y-2">
                            {entry.predictions.map((p, i) => (
                              <ScoreBar key={p.label} label={p.label} score={p.score} rank={i + 1} />
                            ))}
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
                          </div>
                          <dl className="grid shrink-0 grid-cols-2 gap-x-6 gap-y-2 text-sm lg:grid-cols-1">
                            <div>
                              <dt className="text-xs text-ink-faint">Leads</dt>
                              <dd className="font-mono text-ink">{entry.input.leadCount}</dd>
                            </div>
                            <div>
                              <dt className="text-xs text-ink-faint">Sampling rate</dt>
                              <dd className="font-mono text-ink">{entry.input.samplingRateHz} Hz</dd>
                            </div>
                            <div>
                              <dt className="text-xs text-ink-faint">Duration</dt>
                              <dd className="font-mono text-ink">{entry.input.durationSeconds}s</dd>
                            </div>
                            <div>
                              <dt className="text-xs text-ink-faint">Segments</dt>
                              <dd className="font-mono text-ink">{entry.input.segmentsProcessed}</dd>
                            </div>
                          </dl>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </Card>
      )}
    </div>
  );
}
