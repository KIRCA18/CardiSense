import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { House, UploadSimple, Pulse, ClockCounterClockwise, SignOut, List, X } from "@phosphor-icons/react";
import EcgMark from "../ui/EcgMark";
import { useAuth } from "../../lib/auth";

const NAV_ITEMS = [
  { to: "/app", label: "Home", icon: House, end: true },
  { to: "/app/upload", label: "Upload ECGs", icon: UploadSimple },
  { to: "/app/analysis", label: "Analysis", icon: Pulse },
  { to: "/app/history", label: "History", icon: ClockCounterClockwise },
];

export default function AppShell({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const sidebarContent = (
    <>
      <div className="flex items-center justify-between px-6 py-6">
        <div className="flex items-center gap-2">
          <EcgMark className="h-6 w-14 text-primary" strokeWidth={4} />
          <span className="text-[15px] font-semibold tracking-tight text-ink">CardiSense</span>
        </div>
        <button
          type="button"
          onClick={() => setMobileNavOpen(false)}
          aria-label="Close menu"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint hover:bg-surface-alt md:hidden"
        >
          <X size={18} />
        </button>
      </div>

      <nav className="flex flex-1 flex-col gap-1 px-3">
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={() => setMobileNavOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? "bg-primary-soft text-primary-strong"
                  : "text-ink-muted hover:bg-surface-alt hover:text-ink"
              }`
            }
          >
            <Icon size={19} weight="regular" />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-border px-3 py-4">
        <div className="flex items-center gap-3 rounded-lg px-3 py-2">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-sm font-semibold text-primary-strong">
            {(user?.fullName ?? user?.email ?? "?").charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-ink">{user?.fullName || "Researcher"}</p>
            <p className="truncate text-xs text-ink-faint">{user?.email}</p>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            aria-label="Sign out"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint transition-colors hover:bg-surface-alt hover:text-danger"
          >
            <SignOut size={17} />
          </button>
        </div>
      </div>
    </>
  );

  return (
    <div>
      {/* Mobile topbar */}
      <div className="fixed inset-x-0 top-0 z-30 flex items-center justify-between border-b border-border bg-canvas px-4 py-3 md:hidden">
        <div className="flex items-center gap-2">
          <EcgMark className="h-5 w-12 text-primary" strokeWidth={4} />
          <span className="text-sm font-semibold tracking-tight text-ink">CardiSense</span>
        </div>
        <button
          type="button"
          onClick={() => setMobileNavOpen(true)}
          aria-label="Open menu"
          className="flex h-9 w-9 items-center justify-center rounded-lg text-ink-muted hover:bg-surface-alt"
        >
          <List size={20} />
        </button>
      </div>

      {/* Backdrop for mobile drawer */}
      {mobileNavOpen && (
        <div
          className="fixed inset-0 z-40 bg-ink/30 md:hidden"
          onClick={() => setMobileNavOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar: always pinned to the full viewport height (fixed, inset-y-0),
          never a flex sibling that stretches with page content. Slides in as
          a drawer below md; permanently visible at md+. */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-72 max-w-[80vw] flex-col overscroll-contain border-r border-border bg-canvas
          transition-transform duration-200 ease-out
          md:z-auto md:w-64 md:max-w-none md:translate-x-0
          ${mobileNavOpen ? "translate-x-0" : "-translate-x-full"}`}
      >
        {sidebarContent}
      </aside>

      {/* Content area: offset by the sidebar's width at md+, independently
          scrollable and capped to the viewport height so it never drags
          the sidebar's height along with it. */}
      <main className="h-screen overflow-y-auto overflow-x-hidden pt-14 md:pt-0 md:pl-64">
        <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 md:px-8 md:py-10">{children}</div>
      </main>
    </div>
  );
}
