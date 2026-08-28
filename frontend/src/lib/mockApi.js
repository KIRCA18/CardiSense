// Mock backend. Every exported function returns a Promise and has the
// same shape a real fetch() call to the Spring API would have, so wiring
// up the real backend later is a matter of swapping the implementation,
// not the call sites.

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const LABELS = [
  "Normal sinus rhythm",
  "Atrial fibrillation",
  "Sinus bradycardia",
  "Sinus tachycardia",
  "First-degree AV block",
  "Left bundle branch block",
  "Right bundle branch block",
  "Premature ventricular contraction",
  "ST-segment elevation",
  "T-wave inversion",
];

const MODELS = [
  {
    key: "ecg-fm",
    name: "ECG-FM",
    version: "v1.2",
    description: "General-purpose foundation model fine-tuned for 10-class rhythm classification.",
    device: "cuda:0",
    checkpoint: "ecg-fm-base-v1.2.pt",
  },
  {
    key: "resnet-af",
    name: "ResNet-AF",
    version: "v3.0",
    description: "Lightweight residual network specialized for atrial fibrillation screening.",
    device: "cuda:0",
    checkpoint: "resnet-af-v3.0.pt",
  },
  {
    key: "block-net",
    name: "BlockNet",
    version: "v0.9",
    description: "Detects conduction blocks (AV block, bundle branch blocks) from 12-lead input.",
    device: "cpu",
    checkpoint: "blocknet-v0.9.pt",
  },
];

function randomId() {
  return crypto.randomUUID();
}

function seedRecords(datasetName, count, seedOffset) {
  return Array.from({ length: count }, (_, i) => {
    const leadCount = [1, 3, 12][(seedOffset + i) % 3];
    return {
      id: randomId(),
      filename: `${datasetName.toLowerCase().replace(/\s+/g, "-")}-${String(i + 1).padStart(3, "0")}.dat`,
      leadCount,
      sampleCount: 5000 + ((seedOffset + i) % 6) * 1250,
      samplingRateHz: [250, 360, 500][(seedOffset + i) % 3],
      durationSeconds: 10 + ((seedOffset + i) % 5) * 2,
    };
  });
}

let datasets = [
  {
    id: randomId(),
    name: "PTB-XL subset A",
    originalFormat: "WFDB",
    uploadedAt: "2026-08-18T09:14:00Z",
    records: seedRecords("PTB-XL subset A", 24, 0),
  },
  {
    id: randomId(),
    name: "Sinus rhythm cohort",
    originalFormat: "EDF",
    uploadedAt: "2026-08-22T14:02:00Z",
    records: seedRecords("Sinus rhythm cohort", 9, 1),
  },
  {
    id: randomId(),
    name: "Arrhythmia pilot batch",
    originalFormat: "SCP-ECG",
    uploadedAt: "2026-08-25T11:47:00Z",
    records: seedRecords("Arrhythmia pilot batch", 15, 2),
  },
];

function fakePredictionsFor(modelKey) {
  const shuffled = [...LABELS].sort(() => Math.random() - 0.5);
  const top = shuffled.slice(0, 4);
  const raw = top.map(() => Math.random());
  const sum = raw.reduce((a, b) => a + b, 0);
  return top
    .map((label, i) => ({
      label,
      score: raw[i] / sum,
      aggregation: modelKey === "ecg-fm" ? "mean" : "max_vote",
    }))
    .sort((a, b) => b.score - a.score);
}

let history = [
  {
    id: randomId(),
    datasetName: "PTB-XL subset A",
    recordFilename: "ptb-xl-subset-a-004.dat",
    modelKey: "ecg-fm",
    modelName: "ECG-FM",
    status: "completed",
    startedAt: "2026-08-24T10:02:00Z",
    completedAt: "2026-08-24T10:02:41Z",
    input: {
      leadCount: 12,
      sampleCount: 5000,
      samplingRateHz: 500,
      durationSeconds: 10,
      resampledSampleCount: 5000,
      segmentsProcessed: 4,
    },
    predictions: [
      { label: "Normal sinus rhythm", score: 0.82, aggregation: "mean" },
      { label: "Sinus bradycardia", score: 0.09, aggregation: "mean" },
      { label: "T-wave inversion", score: 0.06, aggregation: "mean" },
      { label: "First-degree AV block", score: 0.03, aggregation: "mean" },
    ],
    warnings: [],
  },
  {
    id: randomId(),
    datasetName: "Arrhythmia pilot batch",
    recordFilename: "arrhythmia-pilot-batch-002.dat",
    modelKey: "resnet-af",
    modelName: "ResNet-AF",
    status: "completed",
    startedAt: "2026-08-25T16:30:00Z",
    completedAt: "2026-08-25T16:30:19Z",
    input: {
      leadCount: 3,
      sampleCount: 3750,
      samplingRateHz: 360,
      durationSeconds: 12,
      resampledSampleCount: 4000,
      segmentsProcessed: 3,
    },
    predictions: [
      { label: "Atrial fibrillation", score: 0.71, aggregation: "max_vote" },
      { label: "Normal sinus rhythm", score: 0.19, aggregation: "max_vote" },
      { label: "Sinus tachycardia", score: 0.06, aggregation: "max_vote" },
      { label: "Premature ventricular contraction", score: 0.04, aggregation: "max_vote" },
    ],
    warnings: ["Lead II signal quality below recommended threshold for 1.2s segment."],
  },
  {
    id: randomId(),
    datasetName: "Sinus rhythm cohort",
    recordFilename: "sinus-rhythm-cohort-007.dat",
    modelKey: "block-net",
    modelName: "BlockNet",
    status: "failed",
    startedAt: "2026-08-26T08:15:00Z",
    completedAt: "2026-08-26T08:15:06Z",
    input: {
      leadCount: 1,
      sampleCount: 2500,
      samplingRateHz: 250,
      durationSeconds: 10,
      resampledSampleCount: 0,
      segmentsProcessed: 0,
    },
    predictions: [],
    warnings: [],
    errorMessage: "BlockNet requires at least 3 leads; record provided 1.",
  },
];

// --- Auth (mock — accepts anything, no real backend exists yet) ---

export async function login({ email, password }) {
  await delay(500);
  if (!email || !password) {
    throw new Error("Enter your email and password.");
  }
  return { email, fullName: email.split("@")[0].replace(/[._]/g, " ") };
}

export async function register({ email, password, fullName }) {
  await delay(600);
  if (!email || !password || !fullName) {
    throw new Error("Fill in every field to create your account.");
  }
  return { email, fullName };
}

// Mock verification: a real backend would email an actual code. Here any
// 6-digit code is accepted except "000000", which simulates a wrong code,
// so the error state is reachable during testing/demo.
export async function sendVerificationCode({ email }) {
  await delay(500);
  if (!email) throw new Error("Missing email.");
  return { sent: true };
}

export async function verifyEmail({ email, code }) {
  await delay(500);
  if (!/^\d{6}$/.test(code)) {
    throw new Error("Enter the 6-digit code from your email.");
  }
  if (code === "000000") {
    throw new Error("That code is incorrect or has expired.");
  }
  return { email, verified: true };
}

// --- Datasets ---

export async function listDatasets() {
  await delay(350);
  return datasets.map((d) => ({
    id: d.id,
    name: d.name,
    originalFormat: d.originalFormat,
    uploadedAt: d.uploadedAt,
    recordCount: d.records.length,
  }));
}

export async function listDatasetsWithRecords() {
  await delay(400);
  return datasets;
}

export async function getDataset(id) {
  await delay(250);
  const dataset = datasets.find((d) => d.id === id);
  if (!dataset) throw new Error("Dataset not found.");
  return dataset;
}

export async function uploadDataset({ name, format, files }) {
  await delay(1100);
  const dataset = {
    id: randomId(),
    name: name || `Upload ${new Date().toLocaleDateString()}`,
    originalFormat: format,
    uploadedAt: new Date().toISOString(),
    records: seedRecords(name || "upload", files, Math.floor(Math.random() * 3)),
  };
  datasets = [dataset, ...datasets];
  return dataset;
}

// --- Models ---

export async function listModels() {
  await delay(250);
  return MODELS;
}

// --- Analysis ---

export async function runAnalysis({ recordIds, modelKeys }) {
  await delay(1600);

  const allRecords = datasets.flatMap((d) =>
    d.records.map((r) => ({ ...r, datasetName: d.name })),
  );

  const results = [];
  for (const recordId of recordIds) {
    const record = allRecords.find((r) => r.id === recordId);
    if (!record) continue;

    for (const modelKey of modelKeys) {
      const model = MODELS.find((m) => m.key === modelKey);
      const entry = {
        id: randomId(),
        datasetName: record.datasetName,
        recordFilename: record.filename,
        modelKey,
        modelName: model?.name ?? modelKey,
        status: "completed",
        startedAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
        input: {
          leadCount: record.leadCount,
          sampleCount: record.sampleCount,
          samplingRateHz: record.samplingRateHz,
          durationSeconds: record.durationSeconds,
          resampledSampleCount: record.sampleCount,
          segmentsProcessed: Math.ceil(record.durationSeconds / 3),
        },
        predictions: fakePredictionsFor(modelKey),
        warnings:
          record.leadCount < 3 && modelKey === "block-net"
            ? [`${model?.name ?? modelKey} requires at least 3 leads; record provided ${record.leadCount}.`]
            : [],
      };
      results.push(entry);
    }
  }

  history = [...results, ...history];
  return results;
}

export async function listHistory() {
  await delay(350);
  return history;
}

export async function getHistoryEntry(id) {
  await delay(200);
  const entry = history.find((h) => h.id === id);
  if (!entry) throw new Error("Analysis not found.");
  return entry;
}
