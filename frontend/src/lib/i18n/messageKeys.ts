export type Locale = "en" | "ru";

export const LOCALES: Locale[] = ["en", "ru"];

export type MessageKey =
  | "nav.home"
  | "nav.pricing"
  | "nav.docs"
  | "nav.contact"
  | "nav.dashboard"
  | "nav.watchlist"
  | "nav.alerts"
  | "nav.settings"
  | "nav.login"
  | "nav.register"
  | "landing.title"
  | "landing.subtitle"
  | "landing.addressPlaceholder"
  | "landing.scanButton"
  | "landing.errorInvalidAddress"
  | "landing.errorCreateFailed"
  | "console.title"
  | "console.connecting"
  | "report.verdict"
  | "report.explanation"
  | "report.decisiveSignals"
  | "report.manualChecks"
  | "report.graphTitle"
  | "report.graphPlaceholder"
  | "report.loadError"
  | "comingSoon.body"
  | "notFound.title"
  | "notFound.body";
