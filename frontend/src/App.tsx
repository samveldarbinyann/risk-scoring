import { BrowserRouter, Route, Routes } from "react-router";
import { AppShell } from "@/components/layout/AppShell";
import { LandingPage } from "@/pages/LandingPage";
import { ScanConsolePage } from "@/pages/ScanConsolePage";
import { ReportPage } from "@/pages/ReportPage";
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
          <Route path="/pricing" element={<ComingSoonPage titleKey="nav.pricing" />} />
          <Route path="/docs" element={<ComingSoonPage titleKey="nav.docs" />} />
          <Route path="/contact" element={<ComingSoonPage titleKey="nav.contact" />} />
          <Route path="/dashboard" element={<ComingSoonPage titleKey="nav.dashboard" />} />
          <Route path="/watchlist" element={<ComingSoonPage titleKey="nav.watchlist" />} />
          <Route path="/alerts" element={<ComingSoonPage titleKey="nav.alerts" />} />
          <Route path="/settings" element={<ComingSoonPage titleKey="nav.settings" />} />
          <Route path="/auth" element={<ComingSoonPage titleKey="nav.login" />} />
          <Route path="/register" element={<ComingSoonPage titleKey="nav.register" />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
