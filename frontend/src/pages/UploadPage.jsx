import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { UploadSimple, FolderOpen, FileZip, CheckCircle, X, Pulse } from "@phosphor-icons/react";
import PageHeader from "../components/layout/PageHeader";
import Card from "../components/ui/Card";
import Button from "../components/ui/Button";
import TextField from "../components/ui/TextField";
import EcgMark from "../components/ui/EcgMark";
import * as api from "../lib/mockApi";
import { formatRelativeTime } from "../lib/format";

function detectFormat(files) {
  const names = files.map((f) => f.name.toLowerCase());
  if (names.some((n) => n.endsWith(".dcm"))) return "DICOM";
  if (names.some((n) => n.endsWith(".hea") || n.endsWith(".mat"))) return "WFDB";
  if (
    names.some(
      (n) =>
        n.endsWith(".zip") ||
        n.endsWith(".rar") ||
        n.endsWith(".7z") ||
        n.endsWith(".tar") ||
        n.endsWith(".tar.gz") ||
        n.endsWith(".tgz"),
    )
  ) {
    return "WFDB archive";
  }
  return "Unknown";
}

export default function UploadPage() {
  const [pendingFiles, setPendingFiles] = useState([]);
  const [datasetName, setDatasetName] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploaded, setUploaded] = useState(null);
  const [recent, setRecent] = useState([]);
  const folderInputRef = useRef(null);
  const zipInputRef = useRef(null);

  useEffect(() => {
    api.listDatasets().then(setRecent);
  }, [uploaded]);

  function acceptFiles(fileList) {
    const files = Array.from(fileList);
    if (files.length === 0) return;
    setPendingFiles(files);
    setUploaded(null);
    if (!datasetName) {
      const base = files[0].webkitRelativePath?.split("/")[0] || files[0].name.replace(/\.[^.]+$/, "");
      setDatasetName(base);
    }
  }

  function handleDrop(event) {
    event.preventDefault();
    setIsDragging(false);
    acceptFiles(event.dataTransfer.files);
  }

  async function handleUpload() {
    setUploading(true);
    try {
      const dataset = await api.uploadDataset({
        name: datasetName,
        format: detectFormat(pendingFiles),
        files: pendingFiles.length,
      });
      setUploaded(dataset);
      setPendingFiles([]);
      setDatasetName("");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="animate-fade-up">
      <PageHeader
        title="Upload ECGs"
        description="Upload a WFDB archive or a DICOM ECG waveform file. Individual records inside become selectable in the Analysis page."
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
                {uploaded.records.length} records detected · {uploaded.originalFormat}
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
          ) : pendingFiles.length > 0 ? (
            <div>
              <div className="mb-4 flex items-center justify-between">
                <div>
                  <p className="text-[15px] font-semibold text-ink">
                    {pendingFiles.length} file{pendingFiles.length === 1 ? "" : "s"} ready to upload
                  </p>
                  <p className="text-sm text-ink-muted">Detected format: {detectFormat(pendingFiles)}</p>
                </div>
                <button
                  type="button"
                  onClick={() => setPendingFiles([])}
                  className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint hover:bg-surface-alt hover:text-ink"
                  aria-label="Clear selection"
                >
                  <X size={16} />
                </button>
              </div>

              <ul className="mb-5 max-h-40 divide-y divide-border overflow-y-auto rounded-lg border border-border">
                {pendingFiles.slice(0, 8).map((f, i) => (
                  <li key={i} className="truncate px-3 py-2 font-mono text-xs text-ink-muted">
                    {f.webkitRelativePath || f.name}
                  </li>
                ))}
                {pendingFiles.length > 8 && (
                  <li className="px-3 py-2 text-xs text-ink-faint">+{pendingFiles.length - 8} more</li>
                )}
              </ul>

              <TextField
                id="dataset-name"
                label="Dataset name"
                value={datasetName}
                onChange={(e) => setDatasetName(e.target.value)}
                placeholder="e.g. PTB-XL subset B"
              />

              <Button onClick={handleUpload} disabled={!datasetName} className="mt-4 w-full">
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
                <p className="text-[15px] font-semibold text-ink">Drop a folder, archive, or DICOM file here</p>
                <p className="text-sm text-ink-muted">or choose one of the options below</p>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => folderInputRef.current?.click()}>
                  <FolderOpen size={16} /> Choose folder
                </Button>
                <Button variant="secondary" onClick={() => zipInputRef.current?.click()}>
                  <FileZip size={16} /> Choose file
                </Button>
              </div>
              <p className="text-xs text-ink-faint">Supports WFDB archives and DICOM ECG waveform files</p>

              <input
                ref={folderInputRef}
                type="file"
                webkitdirectory=""
                directory=""
                multiple
                className="hidden"
                onChange={(e) => acceptFiles(e.target.files)}
              />
              <input
                ref={zipInputRef}
                type="file"
                accept=".dcm,application/dicom,.zip,application/zip,.rar,.7z,.tar,.tar.gz,.tgz"
                className="hidden"
                onChange={(e) => acceptFiles(e.target.files)}
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
