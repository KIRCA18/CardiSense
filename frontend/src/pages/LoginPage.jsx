import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import EcgMark from "../components/ui/EcgMark";
import TextField from "../components/ui/TextField";
import Button from "../components/ui/Button";
import { useAuth } from "../lib/auth";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login({ email, password });
      navigate(location.state?.from?.pathname ?? "/app", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not log in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4">
      <div className="w-full max-w-sm animate-fade-up">
        <div className="mb-8 flex flex-col items-center gap-2">
          <Link to="/" aria-label="CardiSense home">
            <EcgMark className="h-6 w-14 text-primary" strokeWidth={4} />
          </Link>
          <h1 className="text-xl font-semibold tracking-tight text-ink">Log in to CardiSense</h1>
          <p className="text-sm text-ink-muted">Access your datasets and past analyses.</p>
        </div>

        <form onSubmit={handleSubmit} className="rounded-2xl border border-border bg-canvas p-6 shadow-[var(--shadow-card)]">
          <div className="flex flex-col gap-4">
            <TextField
              id="email"
              label="Email"
              type="email"
              autoComplete="email"
              spellCheck={false}
              placeholder="you@university.edu"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <TextField
              id="password"
              label="Password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {error && (
              <p role="alert" aria-live="polite" className="text-sm text-danger">
                {error}
              </p>
            )}
            <Button type="submit" disabled={loading} className="mt-1 w-full">
              {loading ? "Logging in…" : "Log in"}
            </Button>
          </div>
        </form>

        <p className="mt-5 text-center text-sm text-ink-muted">
          Don't have an account?{" "}
          <Link to="/register" className="font-medium text-primary hover:text-primary-strong">
            Create one
          </Link>
        </p>
      </div>
    </div>
  );
}
