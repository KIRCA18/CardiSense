import { Check } from "@phosphor-icons/react";

export default function Checkbox({ checked, onChange, className = "", "aria-label": ariaLabel }) {
  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={checked}
      aria-label={ariaLabel}
      onClick={() => onChange(!checked)}
      className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-colors
        ${checked ? "border-primary bg-primary text-white" : "border-border-strong bg-canvas text-transparent hover:border-primary"}
        ${className}`}
    >
      <Check size={13} weight="bold" />
    </button>
  );
}
