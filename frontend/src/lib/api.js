class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function parseErrorMessage(response, fallback) {
  const errorData = await response.json().catch(() => null);
  return errorData?.message || errorData?.detail || fallback;
}

async function throwApiError(response, fallback) {
  const message = await parseErrorMessage(response, fallback);
  throw new ApiError(message, response.status);
}

function normalizeUser(data) {
  const fullName = [data.firstName, data.lastName].filter(Boolean).join(" ");
  return { ...data, fullName };
}

async function getJson(path, fallback) {
  const response = await fetch(path, { credentials: "include" });
  if (!response.ok) {
    await throwApiError(response, fallback);
  }
  return response.json();
}

async function postJson(path, body, fallback) {
  const response = await fetch(path, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    await throwApiError(response, fallback);
  }
  return response.status === 204 ? null : response.json();
}


export async function me() {
  const response = await fetch("/api/auth/me", { credentials: "include" });
  if (!response.ok) {
    throw new ApiError("Not authenticated", response.status);
  }
  return normalizeUser(await response.json());
}

export async function login({ email, password }) {
  if (!email || !password) {
    throw new Error("Enter your email and password.");
  }
  const data = await postJson(
      "/api/auth/log-in",
      { email, password },
      "User not found or invalid credentials",
  );
  return normalizeUser(data);
}

export async function register({ email, password, firstName, lastName }) {
  if (!email || !password || !firstName || !lastName) {
    throw new Error("Fill in every field to create your account.");
  }
  const data = await postJson(
      "/api/auth/sign-up",
      { email, password, firstName, lastName },
      "Registration failed. This email may already be registered.",
  );
  return normalizeUser(data);
}

export async function verifyEmail({ email, code }) {
  const data = await postJson(
      "/api/auth/verify-email",
      { email, code },
      "Could not verify that code.",
  );
  return normalizeUser(data);
}

export async function resendCode({ email }) {
  await postJson(
      "/api/auth/resend-code",
      { email },
      "Couldn't resend the code. Try again in a moment.",
  );
}

export async function listDatasets() {
  return getJson("/api/datasets", "Could not load datasets.");
}

export async function listDatasetsWithRecords() {
  return getJson("/api/datasets", "Could not load datasets.");
}

export async function getDataset(id) {
  return getJson(`/api/datasets/${id}`, "Dataset not found.");
}

export async function uploadDataset({ file }) {
  const form = new FormData();
  form.append("file", file, file.name);
  const response = await fetch("/api/datasets", {
    method: "POST",
    credentials: "include",
    body: form,
  });
  if (!response.ok) {
    await throwApiError(response, "Upload failed. Check that the file is a supported WFDB/DICOM/SCP-ECG upload.");
  }
  return response.json();
}

export async function listModels() {
  return getJson("/api/models", "Could not load models.");
}

export async function runAnalysis({ recordIds, modelKeys }) {
  return postJson(
      "/api/analyses",
      { recordIds, modelKeys },
      "The analysis request failed.",
  );
}

export async function listHistory() {
  return getJson("/api/analyses", "Could not load analysis history.");
}

export async function getHistoryEntry(id) {
  return getJson(`/api/analyses/${id}`, "Analysis not found.");
}
