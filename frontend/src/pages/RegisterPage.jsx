import { useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import EcgMark from "../components/ui/EcgMark"
import TextField from "../components/ui/TextField"
import Button from "../components/ui/Button"
import { useAuth } from "../lib/auth"

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [firstName, setFirstName] = useState("")
  const [lastName, setLastName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError("")
    setLoading(true)
    try {
      await register({ firstName, lastName, email, password })
      navigate("/verify-email", { replace: true, state: { email, firstName, lastName } })
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Could not create your account.",
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4">
      <div className="w-full max-w-sm animate-fade-up">
        <div className="mb-8 flex flex-col items-center gap-2">
          <Link to="/" aria-label="CardiSense home">
            <EcgMark className="h-6 w-14 text-primary" strokeWidth={4} />
          </Link>
          <h1 className="text-xl font-semibold tracking-tight text-ink">
            Create your account
          </h1>
          <p className="text-sm text-ink-muted">
            Upload datasets and run ECG models.
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-2xl border border-border bg-canvas p-6 shadow-[var(--shadow-card)]"
        >
          <div className="flex flex-col gap-4">
            <TextField
              id="firstName"
              label="First name"
              autoComplete="first name"
              placeholder="Example"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              required
            />
            <TextField
              id="lastName"
              label="Last name"
              autoComplete="last name"
              placeholder="Example"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              required
            />
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
              autoComplete="new-password"
              placeholder="At least 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {error && (
              <p
                role="alert"
                aria-live="polite"
                className="text-sm text-danger"
              >
                {error}
              </p>
            )}
            <Button type="submit" disabled={loading} className="mt-1 w-full">
              {loading ? "Creating account…" : "Create account"}
            </Button>
          </div>
        </form>

        <p className="mt-5 text-center text-sm text-ink-muted">
          Already have an account?{" "}
          <Link
            to="/login"
            className="font-medium text-primary hover:text-primary-strong"
          >
            Log in
          </Link>
        </p>
      </div>
    </div>
  )
}
