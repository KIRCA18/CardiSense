import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Stack, Pulse, Cpu, ArrowRight, UploadSimple } from "@phosphor-icons/react";
import PageHeader from "../components/layout/PageHeader";
import Card from "../components/ui/Card";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import EcgMark from "../components/ui/EcgMark";
import { useAuth } from "../lib/auth";
import * as api from "../lib/mockApi";
import { formatRelativeTime, statusTone } from "../lib/format";

export default function HomePage() {
  const { user } = useAuth();
  const [datasets, setDatasets] = useState([]);
  const [history, setHistory] = useState([]);
  const [models, setModels] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([api.listDatasets(), api.listHistory(), api.listModels()]).then(
      ([d, h, m]) => {
        setDatasets(d);
        setHistory(h);
        setModels(m);
        setLoading(false);
      },
    );
  }, []);

  const totalRecords = datasets.reduce((sum, d) => sum + d.recordCount, 0);
  const firstName = (user?.fullName || "").split(" ")[0];

  const stats = [
    { label: "Datasets", value: datasets.length, icon: Stack },
    { label: "Records uploaded", value: totalRecords, icon: EcgMark },
    { label: "Analyses run", value: history.length, icon: Pulse },
    { label: "Models available", value: models.length, icon: Cpu },
  ];

  return (
    <div className="animate-fade-up">
      <PageHeader
        title={firstName ? `Welcome back, ${firstName}` : "Welcome back"}
        description="Here's what's happening across your datasets and model runs."
        action={
          <>
            <Button as={Link} to="/app/upload" variant="secondary">
              <UploadSimple size={16} /> Upload ECGs
            </Button>
            <Button as={Link} to="/app/analysis">
              <Pulse size={16} /> New analysis
            </Button>
          </>
        }
      />

      <div className="mb-8 grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map(({ label, value, icon: Icon }) => (
          <Card key={label} className="min-w-0 p-5">
            <div className="mb-3 flex h-8 w-8 items-center justify-center rounded-lg bg-primary-soft text-primary-strong">
              {Icon === EcgMark ? (
                <Icon className="h-3.5 w-7" strokeWidth={5} />
              ) : (
                <Icon size={16} weight="bold" />
              )}
            </div>
            <p className="text-2xl font-semibold tabular-nums text-ink">{loading ? "—" : value}</p>
            <p className="text-sm text-ink-muted">{label}</p>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="min-w-0 p-6">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-[15px] font-semibold text-ink">Recent datasets</h2>
            <Link to="/app/upload" className="flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-strong">
              View all <ArrowRight size={14} />
            </Link>
          </div>
          <ul className="flex flex-col divide-y divide-border">
            {datasets.slice(0, 4).map((d) => (
              <li key={d.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-ink">{d.name}</p>
                  <p className="text-xs text-ink-faint">
                    {d.recordCount} records · {d.originalFormat} · {formatRelativeTime(d.uploadedAt)}
                  </p>
                </div>
                <Badge tone="neutral">{d.originalFormat}</Badge>
              </li>
            ))}
            {!loading && datasets.length === 0 && (
              <p className="py-6 text-center text-sm text-ink-faint">No datasets uploaded yet.</p>
            )}
          </ul>
        </Card>

        <Card className="min-w-0 p-6">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-[15px] font-semibold text-ink">Recent analyses</h2>
            <Link to="/app/history" className="flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-strong">
              View all <ArrowRight size={14} />
            </Link>
          </div>
          <ul className="flex flex-col divide-y divide-border">
            {history.slice(0, 4).map((h) => (
              <li key={h.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-ink">{h.recordFilename}</p>
                  <p className="text-xs text-ink-faint">
                    {h.modelName} · {formatRelativeTime(h.startedAt)}
                  </p>
                </div>
                <Badge tone={statusTone(h.status)}>{h.status}</Badge>
              </li>
            ))}
            {!loading && history.length === 0 && (
              <p className="py-6 text-center text-sm text-ink-faint">No analyses run yet.</p>
            )}
          </ul>
        </Card>
      </div>
    </div>
  );
}
