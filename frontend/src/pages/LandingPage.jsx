import { Link } from "react-router-dom";
import { UploadSimple, Pulse, ShieldCheck } from "@phosphor-icons/react";
import EcgMark from "../components/ui/EcgMark";
import Button from "../components/ui/Button";

const FEATURES = [
  {
    icon: UploadSimple,
    title: "Bring your own dataset",
    description: "Upload folders or ZIP archives of WFDB, EDF, or SCP-ECG records as a batch.",
  },
  {
    icon: Pulse,
    title: "Run any model you need",
    description: "Select one record or a whole cohort, choose one or more models, compare results side by side.",
  },
  {
    icon: ShieldCheck,
    title: "Anonymous by design",
    description: "Records are stored and analyzed without patient identity attached to the signal data.",
  },
];

export default function LandingPage() {
  return (
    <div className="relative min-h-screen overflow-hidden">
      <EcgMark className="pointer-events-none absolute -right-10 top-24 h-40 w-[36rem] text-primary opacity-[0.06]" strokeWidth={2} />

      <header className="relative mx-auto flex max-w-6xl items-center justify-between px-8 py-6">
        <div className="flex items-center gap-2">
          <EcgMark className="h-6 w-14 text-primary" strokeWidth={4} />
          <span className="text-[15px] font-semibold tracking-tight text-ink">CardiSense</span>
        </div>
        <div className="flex items-center gap-2">
          <Button as={Link} to="/login" variant="ghost" size="sm">
            Log in
          </Button>
          <Button as={Link} to="/register" variant="primary" size="sm">
            Create account
          </Button>
        </div>
      </header>

      <section className="relative mx-auto max-w-3xl px-8 pb-24 pt-20 text-center">
        <p className="mb-4 text-xs font-semibold uppercase tracking-wide text-primary">ECG research platform</p>
        <h1 className="text-3xl font-semibold tracking-tight text-ink sm:text-4xl md:text-5xl">
          Run ECG models on your datasets, without the glue code.
        </h1>
        <p className="mx-auto mt-5 max-w-xl text-[17px] leading-relaxed text-ink-muted">
          Upload a batch of records, pick a model or three, and get structured predictions back —
          built for researchers comparing architectures across their own cohorts.
        </p>
        <div className="mt-8 flex items-center justify-center gap-3">
          <Button as={Link} to="/register" size="lg">
            Create account
          </Button>
          <Button as={Link} to="/login" variant="secondary" size="lg">
            Log in
          </Button>
        </div>
      </section>

      <section className="relative mx-auto max-w-5xl px-8 pb-24">
        <div className="grid gap-4 sm:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, description }) => (
            <div key={title} className="rounded-2xl border border-border bg-canvas p-6">
              <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-primary-soft text-primary-strong">
                <Icon size={20} weight="regular" />
              </div>
              <h3 className="text-[15px] font-semibold text-ink">{title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-ink-muted">{description}</p>
            </div>
          ))}
        </div>
      </section>

      <footer className="relative border-t border-border px-8 py-6 text-center text-xs text-ink-faint">
        CardiSense — a research project for anonymized ECG model inference.
      </footer>
    </div>
  );
}
