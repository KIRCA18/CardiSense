const PATH =
  "M0 20 H14 L20 6 L28 34 L36 12 L42 20 H56 L62 8 L68 28 L74 20 H90";

export default function EcgMark({ animated = false, className = "", strokeWidth = 3 }) {
  return (
    <svg
      viewBox="0 0 90 40"
      className={`${animated ? "ecg-trace" : ""} ${className}`}
      fill="none"
      aria-hidden="true"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d={PATH}
        stroke="currentColor"
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
