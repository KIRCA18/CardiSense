import { Routes, Route } from "react-router-dom";
import RequireAuth from "./components/layout/RequireAuth";
import RedirectIfAuthenticated from "./components/layout/RedirectIfAuthenticated";
import AppShell from "./components/layout/AppShell";
import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import VerifyEmailPage from "./pages/VerifyEmailPage";
import HomePage from "./pages/HomePage";
import UploadPage from "./pages/UploadPage";
import AnalysisPage from "./pages/AnalysisPage";
import HistoryPage from "./pages/HistoryPage";

function Protected({ children }) {
    return (
        <RequireAuth>
            <AppShell>{children}</AppShell>
        </RequireAuth>
    );
}

function Public({ children }) {
    return <RedirectIfAuthenticated>{children}</RedirectIfAuthenticated>;
}

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Public><LoginPage /></Public>} />
            <Route path="/register" element={<Public><RegisterPage /></Public>} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />

            <Route path="/app" element={<Protected><HomePage /></Protected>} />
            <Route path="/app/upload" element={<Protected><UploadPage /></Protected>} />
            <Route path="/app/analysis" element={<Protected><AnalysisPage /></Protected>} />
            <Route path="/app/history" element={<Protected><HistoryPage /></Protected>} />
        </Routes>
    );
}