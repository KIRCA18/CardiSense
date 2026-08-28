import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../lib/auth";
import EcgMark from "../ui/EcgMark";

export default function RequireAuth({ children }) {
  const { user } = useAuth();
  const location = useLocation();

  if (user === undefined) {
    // Session is still being resolved from storage.
    return (
      <div className="flex min-h-screen items-center justify-center">
        <EcgMark animated className="h-8 w-20 text-primary" strokeWidth={4} />
        <span className="sr-only" role="status" aria-live="polite">
          Loading your session…
        </span>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
