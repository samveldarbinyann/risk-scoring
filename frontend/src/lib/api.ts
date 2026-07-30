import type {
  AlertView,
  AuthResponse,
  ChainCandidatesResponse,
  ErrorResponse,
  LoginRequest,
  RegisterRequest,
  RegistrationResponse,
  ResendCodeRequest,
  ScanCreateRequest,
  ScanGroupAcceptedResponse,
  ScanGroupReportView,
  ScanGroupView,
  UserView,
  VerifyEmailRequest,
  WatchlistCreateRequest,
  WatchlistEntryView,
} from "@/lib/types";
import type { Locale } from "@/lib/i18n/messageKeys";

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

const AUTH_PATH_PREFIX = "/api/auth";
// /me is the only auth endpoint that requires an already-issued access token;
// login/register/verify/resend/refresh/logout are called without one.
const NO_RETRY_PATHS = ["/api/auth/login", "/api/auth/refresh", "/api/auth/register", "/api/auth/logout"];

let currentLocale: Locale = "en";

export function setApiLocale(locale: Locale): void {
  currentLocale = locale;
}

// Access token lives only in memory for the lifetime of the tab. It is never
// persisted (no localStorage/sessionStorage/cookie) so it cannot be read by
// an XSS payload that survives a reload. The refresh token is a separate,
// httpOnly, SameSite=Lax cookie the browser attaches automatically on
// same-site requests — this module never reads or writes it directly.
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

type SessionExpiredListener = () => void;
const sessionExpiredListeners = new Set<SessionExpiredListener>();

// Called when a request fails with 401 and the follow-up silent refresh also
// fails — i.e. the session is truly gone. AuthProvider subscribes to this to
// drop the in-memory user/token state.
export function onSessionExpired(listener: SessionExpiredListener): () => void {
  sessionExpiredListeners.add(listener);
  return () => sessionExpiredListeners.delete(listener);
}

function notifySessionExpired(): void {
  setAccessToken(null);
  sessionExpiredListeners.forEach((listener) => listener());
}

// Deduplicates concurrent refresh attempts: if several requests hit 401 at
// once, only one refresh call is made and the rest await the same promise.
let refreshInFlight: Promise<boolean> | null = null;

function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = rawRequest<AuthResponse>(`${AUTH_PATH_PREFIX}/refresh`, { method: "POST" })
      .then((session) => {
        setAccessToken(session.accessToken);
        return true;
      })
      .catch(() => {
        notifySessionExpired();
        return false;
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

async function parseErrorMessage(response: Response): Promise<string> {
  const body = (await response.json().catch(() => null)) as ErrorResponse | null;
  return body?.message ?? `Request failed with status ${response.status}`;
}

export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function rawRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    // Cross-port but same-site in dev; required so the browser sends/accepts
    // the httpOnly refresh-token cookie. Server enforces allowCredentials
    // with an explicit CORS origin allow-list, not a wildcard.
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "Accept-Language": currentLocale,
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    throw new ApiError(await parseErrorMessage(response), response.status);
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    return await rawRequest<T>(path, init);
  } catch (error) {
    const isUnauthorized = error instanceof ApiError && error.status === 401;
    if (!isUnauthorized || NO_RETRY_PATHS.includes(path) || !accessToken) {
      throw error;
    }

    const refreshed = await refreshSession();
    if (!refreshed) {
      throw error;
    }

    return rawRequest<T>(path, init);
  }
}

export function getChainCandidates(address: string): Promise<ChainCandidatesResponse> {
  return apiRequest<ChainCandidatesResponse>(`/api/chains/candidates?address=${encodeURIComponent(address)}`);
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

export function getMessages(locale: Locale, init?: RequestInit): Promise<Record<string, string>> {
  return apiRequest<Record<string, string>>("/api/i18n", {
    ...init,
    headers: { ...init?.headers, "Accept-Language": locale },
  });
}

export function register(payload: RegisterRequest): Promise<RegistrationResponse> {
  return apiRequest<RegistrationResponse>(`${AUTH_PATH_PREFIX}/register`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function verifyEmail(payload: VerifyEmailRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>(`${AUTH_PATH_PREFIX}/verify-email`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function resendCode(payload: ResendCodeRequest): Promise<void> {
  return apiRequest<void>(`${AUTH_PATH_PREFIX}/resend-code`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function login(payload: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>(`${AUTH_PATH_PREFIX}/login`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

// Uses the httpOnly refresh-token cookie only; never called with a stale
// access token attached on purpose (path is in NO_RETRY_PATHS).
export function refresh(): Promise<AuthResponse> {
  return apiRequest<AuthResponse>(`${AUTH_PATH_PREFIX}/refresh`, { method: "POST" });
}

export function logout(): Promise<void> {
  return apiRequest<void>(`${AUTH_PATH_PREFIX}/logout`, { method: "POST" });
}

export function getMe(): Promise<UserView> {
  return apiRequest<UserView>(`${AUTH_PATH_PREFIX}/me`);
}

export function listWatchlist(): Promise<WatchlistEntryView[]> {
  return apiRequest<WatchlistEntryView[]>("/api/watchlist");
}

export function addToWatchlist(payload: WatchlistCreateRequest): Promise<void> {
  return apiRequest<void>("/api/watchlist", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function removeFromWatchlist(id: string): Promise<void> {
  return apiRequest<void>(`/api/watchlist/${id}`, { method: "DELETE" });
}

export function listAlerts(): Promise<AlertView[]> {
  return apiRequest<AlertView[]>("/api/alerts");
}
