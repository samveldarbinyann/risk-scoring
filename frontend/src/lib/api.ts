import type {
  ErrorResponse,
  ScanCreateRequest,
  ScanGroupAcceptedResponse,
  ScanGroupReportView,
  ScanGroupView,
} from "@/lib/types";
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

export function createScan(payload: ScanCreateRequest): Promise<ScanGroupAcceptedResponse> {
  return apiRequest<ScanGroupAcceptedResponse>("/api/scans", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getScanGroup(groupId: string): Promise<ScanGroupView> {
  return apiRequest<ScanGroupView>(`/api/scans/groups/${groupId}`);
}

export function getScanGroupReport(groupId: string): Promise<ScanGroupReportView> {
  return apiRequest<ScanGroupReportView>(`/api/scans/groups/${groupId}/report`);
}

export function getMessages(): Promise<Record<string, string>> {
  return apiRequest<Record<string, string>>("/api/i18n");
}
