import { BrowserRouter, Route, Routes } from "react-router";
import { AppShell } from "@/components/layout/AppShell";
import { LandingPage } from "@/pages/LandingPage";
import { ScanConsolePage } from "@/pages/ScanConsolePage";
import { ReportPage } from "@/pages/ReportPage";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { WatchlistPage } from "@/pages/WatchlistPage";
import { AlertsPage } from "@/pages/AlertsPage";
import { PricingPage } from "@/pages/PricingPage";
import { PaymentPage } from "@/pages/PaymentPage";
import { DocsPage } from "@/pages/DocsPage";
import { ContactPage } from "@/pages/ContactPage";
import { SettingsPage } from "@/pages/SettingsPage";
import { DashboardPage } from "@/pages/DashboardPage";
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
          <Route path="/pricing/pay" element={<PaymentPage />} />
          <Route path="/docs" element={<DocsPage />} />
          <Route path="/contact" element={<ContactPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/watchlist" element={<WatchlistPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/auth" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
