import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { UploadSimple, FileArchive, CheckCircle, X, Pulse } from "@phosphor-icons/react";
import PageHeader from "../components/layout/PageHeader";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import EcgMark from "../components/ui/EcgMark";
import * as api from "../lib/api";
import { formatRelativeTime } from "../lib/format";

const ACCEPTED_EXTENSIONS = [".zip", ".rar", ".7z", ".tar", ".tar.gz", ".tgz", ".dcm", ".scp"];

function detectFormat(filename) {
  const name = filename.toLowerCase();
  if (name.endsWith(".dcm")) return "DICOM";
  if (name.endsWith(".scp")) return "SCP-ECG";
  if (ACCEPTED_EXTENSIONS.some((ext) => ext !== ".dcm" && ext !== ".scp" && name.endsWith(ext))) {
    return "Archive (WFDB/DICOM/SCP-ECG)";
  }
  return "Unsupported";
}

export default function UploadPage() {
  const [pendingFile, setPendingFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploaded, setUploaded] = useState(null);
  const [uploadError, setUploadError] = useState("");
  const [recent, setRecent] = useState([]);
  const fileInputRef = useRef(null);

  useEffect(() => {
    api.listDatasets().then(setRecent).catch(() => setRecent([]));
  }, [uploaded]);

  function acceptFile(fileList) {
    const file = fileList?.[0];
    if (!file) return;
    setPendingFile(file);
    setUploaded(null);
    setUploadError("");
  }

  function handleDrop(event) {
    event.preventDefault();
    setIsDragging(false);
    acceptFile(event.dataTransfer.files);
  }

  async function handleUpload() {
    setUploading(true);
    setUploadError("");
    try {
      const dataset = await api.uploadDataset({ file: pendingFile });
      setUploaded(dataset);
      setPendingFile(null);
    } catch (err) {
      setUploadError(
          err instanceof Error ? err.message : "Upload failed. Please try again.",
      );
    } finally {
      setUploading(false);
    }
  }

  return (
      <div className="animate-fade-up">
        <PageHeader
            title="Upload ECGs"
            description="Upload one WFDB/DICOM/SCP-ECG archive (or a single .dcm/.scp file) as a dataset. Individual records inside become selectable in the Analysis page."
        />

        <div className="grid gap-6 lg:grid-cols-[1.3fr_1fr]">
          <Card className="min-w-0 p-6">
            {uploading ? (
                <div className="flex flex-col items-center justify-center gap-4 py-16">
                  <EcgMark animated className="h-10 w-24 text-primary" strokeWidth={3.5} />
                  <p className="text-sm text-ink-muted">Reading and standardizing records…</p>
                </div>
            ) : uploaded ? (
                <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-success-soft text-success">
                    <CheckCircle size={26} weight="fill" />
                  </div>
                  <p className="text-[15px] font-semibold text-ink">"{uploaded.name}" uploaded</p>
                  <p className="text-sm text-ink-muted">
                    {uploaded.records.length} record{uploaded.records.length === 1 ? "" : "s"} detected · {uploaded.originalFormat}
                  </p>
                  <div className="mt-2 flex gap-2">
                    <Button variant="secondary" onClick={() => setUploaded(null)}>
                      Upload another
                    </Button>
                    <Button as={Link} to="/app/analysis">
                      <Pulse size={16} /> Analyze this dataset
                    </Button>
                  </div>
                </div>
            ) : pendingFile ? (
                <div>
                  <div className="mb-4 flex items-center justify-between">
                    <div>
                      <p className="text-[15px] font-semibold text-ink">Ready to upload</p>
                      <p className="text-sm text-ink-muted">Detected format: {detectFormat(pendingFile.name)}</p>
                    </div>
                    <button
                        type="button"
                        onClick={() => setPendingFile(null)}
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint hover:bg-surface-alt hover:text-ink"
                        aria-label="Clear selection"
                    >
                      <X size={16} />
                    </button>
                  </div>

                  <p className="mb-5 truncate rounded-lg border border-border px-3 py-2 font-mono text-xs text-ink-muted">
                    {pendingFile.name}
                  </p>

                  {uploadError && (
                      <p role="alert" className="mb-3 text-sm text-danger">
                        {uploadError}
                      </p>
                  )}

                  <Button onClick={handleUpload} className="w-full">
                    Upload dataset
                  </Button>
                </div>
            ) : (
                <div
                    onDragOver={(e) => {
                      e.preventDefault();
                      setIsDragging(true);
                    }}
                    onDragLeave={() => setIsDragging(false)}
                    onDrop={handleDrop}
                    className={`flex flex-col items-center justify-center gap-4 rounded-xl border-2 border-dashed px-6 py-16 text-center transition-colors ${
                        isDragging ? "border-primary bg-primary-soft" : "border-border"
                    }`}
                >
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-soft text-primary-strong">
                    <UploadSimple size={22} />
                  </div>
                  <div>
                    <p className="text-[15px] font-semibold text-ink">Drop an archive or ECG file here</p>
                    <p className="text-sm text-ink-muted">or choose one below</p>
                  </div>
                  <Button variant="secondary" onClick={() => fileInputRef.current?.click()}>
                    <FileArchive size={16} /> Choose file
                  </Button>
                  <p className="text-xs text-ink-faint">
                    WFDB archives (.zip/.rar/.7z/.tar/.tar.gz), or a single DICOM (.dcm) or SCP-ECG (.scp) file
                  </p>

                  <input
                      ref={fileInputRef}
                      type="file"
                      accept={ACCEPTED_EXTENSIONS.join(",")}
                      className="hidden"
                      onChange={(e) => acceptFile(e.target.files)}
                  />
                </div>
            )}
          </Card>

          <Card className="min-w-0 p-6">
            <h2 className="mb-4 text-[15px] font-semibold text-ink">Recently uploaded</h2>
            <ul className="flex flex-col divide-y divide-border">
              {recent.slice(0, 6).map((d) => (
                  <li key={d.id} className="py-3">
                    <p className="truncate text-sm font-medium text-ink">{d.name}</p>
                    <p className="text-xs text-ink-faint">
                      {d.recordCount} records · {formatRelativeTime(d.uploadedAt)}
                    </p>
                  </li>
              ))}
              {recent.length === 0 && <p className="py-6 text-center text-sm text-ink-faint">No datasets yet.</p>}
            </ul>
          </Card>
        </div>
      </div>
  );
}
