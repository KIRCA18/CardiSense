const VARIANTS = {
  primary:
    "bg-primary text-white hover:bg-primary-strong disabled:bg-border-strong disabled:text-ink-faint",
  secondary:
    "bg-canvas text-ink border border-border hover:border-border-strong hover:bg-surface-alt disabled:text-ink-faint",
  ghost: "bg-transparent text-ink-muted hover:bg-surface-alt hover:text-ink",
  danger: "bg-danger text-white hover:bg-danger/90 disabled:bg-border-strong disabled:text-ink-faint",
};

const SIZES = {
  sm: "h-8 px-3 text-sm gap-1.5",
  md: "h-10 px-4 text-sm gap-2",
  lg: "h-11 px-5 text-[15px] gap-2",
};

export default function Button({
  variant = "primary",
  size = "md",
  className = "",
  children,
  as: Component = "button",
  ...props
}) {
  return (
    <Component
      className={`inline-flex items-center justify-center rounded-lg font-medium
        transition-colors duration-150 active:scale-[0.97] disabled:cursor-not-allowed
        disabled:active:scale-100 whitespace-nowrap
        ${VARIANTS[variant]} ${SIZES[size]} ${className}`}
      {...props}
    >
      {children}
    </Component>
  );
}
