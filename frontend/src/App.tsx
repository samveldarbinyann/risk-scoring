import { lazy } from "react";
import { BrowserRouter, Route, Routes } from "react-router";
import { AppShell } from "@/components/layout/AppShell";

const LandingPage = lazy(() => import("@/pages/LandingPage").then((m) => ({ default: m.LandingPage })));
const ScanConsolePage = lazy(() => import("@/pages/ScanConsolePage").then((m) => ({ default: m.ScanConsolePage })));
const ReportPage = lazy(() => import("@/pages/ReportPage").then((m) => ({ default: m.ReportPage })));
const LoginPage = lazy(() => import("@/pages/LoginPage").then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import("@/pages/RegisterPage").then((m) => ({ default: m.RegisterPage })));
const ForgotPasswordPage = lazy(() =>
  import("@/pages/ForgotPasswordPage").then((m) => ({ default: m.ForgotPasswordPage })),
);
const WatchlistPage = lazy(() => import("@/pages/WatchlistPage").then((m) => ({ default: m.WatchlistPage })));
const AlertsPage = lazy(() => import("@/pages/AlertsPage").then((m) => ({ default: m.AlertsPage })));
const PricingPage = lazy(() => import("@/pages/PricingPage").then((m) => ({ default: m.PricingPage })));
const PaymentPage = lazy(() => import("@/pages/PaymentPage").then((m) => ({ default: m.PaymentPage })));
const DocsPage = lazy(() => import("@/pages/DocsPage").then((m) => ({ default: m.DocsPage })));
const ContactPage = lazy(() => import("@/pages/ContactPage").then((m) => ({ default: m.ContactPage })));
const SettingsPage = lazy(() => import("@/pages/SettingsPage").then((m) => ({ default: m.SettingsPage })));
const DashboardPage = lazy(() => import("@/pages/DashboardPage").then((m) => ({ default: m.DashboardPage })));
const NotFoundPage = lazy(() => import("@/pages/NotFoundPage").then((m) => ({ default: m.NotFoundPage })));

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
