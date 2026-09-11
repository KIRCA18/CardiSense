class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function parseErrorMessage(response, fallback) {
  const errorData = await response.json().catch(() => null);
  return errorData?.message || fallback;
}

async function throwApiError(response, fallback) {
  const message = await parseErrorMessage(response, fallback);
  throw new ApiError(message, response.status);
}

function normalizeUser(data) {
  const fullName = [data.firstName, data.lastName].filter(Boolean).join(" ");
  return { ...data, fullName };
}

export async function login({ email, password }) {
  if (!email || !password) {
    throw new Error("Enter your email and password.");
  }
  const response = await fetch("/api/auth/log-in", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) {
    await throwApiError(response, "User not found or invalid credentials");
  }
  return normalizeUser(await response.json());
}

export async function register({ email, password, firstName, lastName }) {
  if (!email || !password || !firstName || !lastName) {
    throw new Error("Fill in every field to create your account.");
  }
  const response = await fetch("/api/auth/sign-up", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, firstName, lastName }),
  });
  if (!response.ok) {
    await throwApiError(response, "Registration failed. This email may already be registered.");
  }
  return normalizeUser(await response.json());
}

export async function verifyEmail({ email, code }) {
  const response = await fetch("/api/auth/verify-email", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, code }),
  });
  if (!response.ok) {
    await throwApiError(response, "Could not verify that code.");
  }
  return normalizeUser(await response.json());
}

export async function resendCode({ email }) {
  const response = await fetch("/api/auth/resend-code", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });
  if (!response.ok) {
    await throwApiError(response, "Couldn't resend the code. Try again in a moment.");
  }
}