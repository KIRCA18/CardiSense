export default function ScoreBar({ label, score, rank }) {
  const pct = Math.round(score * 1000) / 10;
  return (
    <div className="flex items-center gap-3">
      <span className="w-5 shrink-0 text-xs font-medium text-ink-faint">{rank}</span>
      <span className="w-64 shrink-0 text-sm text-ink" title={label}>
        {label}
      </span>
      <div className="h-2 flex-1 overflow-hidden rounded-full bg-surface-alt">
        <div
          className="h-full rounded-full bg-primary transition-all duration-500"
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="w-14 shrink-0 text-right font-mono text-sm text-ink-muted">{pct.toFixed(1)}%</span>
    </div>
  );
}
