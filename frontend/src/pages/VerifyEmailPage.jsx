import { useEffect, useRef, useState } from "react"
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom"
import { EnvelopeSimple } from "@phosphor-icons/react"
import EcgMark from "../components/ui/EcgMark"
import Button from "../components/ui/Button"
import { useAuth } from "../lib/auth"
import * as api from "../lib/api"

const CODE_LENGTH = 6
const RESEND_COOLDOWN_SECONDS = 30

export default function VerifyEmailPage() {
  const { verifyEmail } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { email } = location.state ?? {}

  const [digits, setDigits] = useState(Array(CODE_LENGTH).fill(""))
  const [error, setError] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [resendCooldown, setResendCooldown] = useState(RESEND_COOLDOWN_SECONDS)
  const [resending, setResending] = useState(false)
  const inputRefs = useRef([])

  useEffect(() => {
    inputRefs.current[0]?.focus()
  }, [])

  useEffect(() => {
    if (resendCooldown <= 0) return
    const timer = setTimeout(() => setResendCooldown((s) => s - 1), 1000)
    return () => clearTimeout(timer)
  }, [resendCooldown])

  // Registered without an email in state (e.g. direct navigation) — nothing
  // to verify, send them back to start over.
  if (!email) {
    return <Navigate to="/register" replace />
  }

  function setDigitAt(index, value) {
    setDigits((prev) => {
      const next = [...prev]
      next[index] = value
      return next
    })
  }

  function handleChange(index, rawValue) {
    const value = rawValue.replace(/\D/g, "").slice(-1)
    setDigitAt(index, value)
    if (value && index < CODE_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }

  function handleKeyDown(index, event) {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  function handlePaste(event) {
    const pasted = event.clipboardData
      .getData("text")
      .replace(/\D/g, "")
      .slice(0, CODE_LENGTH)
    if (!pasted) return
    event.preventDefault()
    setDigits((prev) => {
      const next = [...prev]
      for (let i = 0; i < pasted.length; i++) next[i] = pasted[i]
      return next
    })
    inputRefs.current[Math.min(pasted.length, CODE_LENGTH - 1)]?.focus()
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError("")
    const code = digits.join("")
    if (code.length < CODE_LENGTH) {
      setError("Enter all 6 digits.")
      return
    }
    setSubmitting(true)
    try {
      await verifyEmail({ email, code });
      navigate("/app", { replace: true })
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Could not verify that code.",
      )
      setDigits(Array(CODE_LENGTH).fill(""))
      inputRefs.current[0]?.focus()
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResend() {
    setResending(true)
    setError("")
    try {
      await api.resendCode({ email });
      setResendCooldown(RESEND_COOLDOWN_SECONDS)
    } catch {
      setError("Couldn't resend the code. Try again in a moment.")
    } finally {
      setResending(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4">
      <div className="w-full max-w-sm animate-fade-up">
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <Link to="/" aria-label="CardiSense home">
            <EcgMark className="h-6 w-14 text-primary" strokeWidth={4} />
          </Link>
          <div className="mt-2 flex h-11 w-11 items-center justify-center rounded-full bg-primary-soft text-primary-strong">
            <EnvelopeSimple size={20} />
          </div>
          <h1 className="text-xl font-semibold tracking-tight text-ink">
            Check your email
          </h1>
          <p className="text-sm text-ink-muted">
            We sent a 6-digit code to{" "}
            <span className="font-medium text-ink">{email}</span>
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-2xl border border-border bg-canvas p-6 shadow-[var(--shadow-card)]"
        >
          <fieldset>
            <legend className="mb-3 text-sm font-medium text-ink">
              Verification code
            </legend>
            <div className="flex justify-between gap-2" onPaste={handlePaste}>
              {digits.map((digit, index) => (
                <input
                  key={index}
                  ref={(el) => (inputRefs.current[index] = el)}
                  value={digit}
                  onChange={(e) => handleChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  inputMode="numeric"
                  autoComplete={index === 0 ? "one-time-code" : "off"}
                  maxLength={1}
                  aria-label={`Digit ${index + 1} of 6`}
                  className="h-12 w-11 rounded-lg border border-border bg-canvas text-center text-lg font-semibold text-ink
                    transition-colors focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary-soft"
                />
              ))}
            </div>
          </fieldset>

          {error && (
            <p
              role="alert"
              aria-live="polite"
              className="mt-3 text-sm text-danger"
            >
              {error}
            </p>
          )}

          <Button type="submit" disabled={submitting} className="mt-5 w-full">
            {submitting ? "Verifying…" : "Verify email"}
          </Button>
        </form>

        <p className="mt-5 text-center text-sm text-ink-muted">
          Didn't get a code?{" "}
          {resendCooldown > 0 ? (
            <span className="text-ink-faint">Resend in {resendCooldown}s</span>
          ) : (
            <button
              type="button"
              onClick={handleResend}
              disabled={resending}
              className="font-medium text-primary hover:text-primary-strong disabled:text-ink-faint"
            >
              {resending ? "Sending…" : "Resend code"}
            </button>
          )}
        </p>
      </div>
    </div>
  )
}
