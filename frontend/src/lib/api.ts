import type { ErrorResponse, ScanAcceptedResponse, ScanCreateRequest, ScanReportView, ScanView } from "@/lib/types";
import type { Locale } from "@/lib/i18n/messageKeys";

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

let currentLocale: Locale = "en";

export function setApiLocale(locale: Locale): void {
  currentLocale = locale;
}

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      "Accept-Language": currentLocale,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ErrorResponse | null;
    throw new Error(body?.message ?? `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function createScan(payload: ScanCreateRequest): Promise<ScanAcceptedResponse> {
  return apiRequest<ScanAcceptedResponse>("/api/scans", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getScan(scanId: string): Promise<ScanView> {
  return apiRequest<ScanView>(`/api/scans/${scanId}`);
}

export function getScanReport(scanId: string): Promise<ScanReportView> {
  return apiRequest<ScanReportView>(`/api/scans/${scanId}/report`);
}

export function getMessages(): Promise<Record<string, string>> {
  return apiRequest<Record<string, string>>("/api/i18n");
}
