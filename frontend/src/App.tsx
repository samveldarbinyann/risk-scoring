import { BrowserRouter, Route, Routes } from "react-router";
import { AppShell } from "@/components/layout/AppShell";
import { LandingPage } from "@/pages/LandingPage";
import { ScanConsolePage } from "@/pages/ScanConsolePage";
import { ReportPage } from "@/pages/ReportPage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { WatchlistPage } from "@/pages/WatchlistPage";
import { AlertsPage } from "@/pages/AlertsPage";
import { PricingPage } from "@/pages/PricingPage";
import { SettingsPage } from "@/pages/SettingsPage";
import { ComingSoonPage } from "@/pages/ComingSoonPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<LandingPage />} />
          <Route path="/scan/:groupId" element={<ScanConsolePage />} />
          <Route path="/scan/:groupId/report" element={<ReportPage />} />
          <Route path="/pricing" element={<PricingPage />} />
          <Route path="/docs" element={<ComingSoonPage titleKey="nav.docs" />} />
          <Route path="/contact" element={<ComingSoonPage titleKey="nav.contact" />} />
          <Route path="/dashboard" element={<ComingSoonPage titleKey="nav.dashboard" />} />
          <Route path="/watchlist" element={<WatchlistPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/auth" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
