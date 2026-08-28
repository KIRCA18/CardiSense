export default function Card({ className = "", children, ...props }) {
  return (
    <div
      className={`rounded-2xl border border-border bg-canvas shadow-[var(--shadow-card)] ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}
