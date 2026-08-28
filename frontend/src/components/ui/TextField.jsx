export default function TextField({ label, id, error, hint, className = "", ...props }) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-ink">
          {label}
        </label>
      )}
      <input
        id={id}
        className={`h-11 rounded-lg border bg-canvas px-3.5 text-[15px] text-ink placeholder:text-ink-faint
          transition-colors focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary-soft
          ${error ? "border-danger" : "border-border"}`}
        {...props}
      />
      {error ? (
        <p className="text-xs text-danger">{error}</p>
      ) : hint ? (
        <p className="text-xs text-ink-faint">{hint}</p>
      ) : null}
    </div>
  );
}
