export type ScanStage = "PENDING" | "FETCHING" | "ENRICHING" | "ANALYZING" | "COMPLETED" | "FAILED";
export type ScanSource = "USER" | "MONITOR";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type UserRole = "USER" | "ADMIN";
export type UserStatus = "PENDING_VERIFICATION" | "ACTIVE" | "BLOCKED";
export type Language = "EN" | "RU";

export interface UserView {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  language: Language;
}

export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  user: UserView;
}

export interface RegisterRequest {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface RegistrationResponse {
  email: string;
  status: UserStatus;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface ResendCodeRequest {
  email: string;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface ScanCreateRequest {
  address: string;
  chainIds?: number[];
}

export interface ScanGroupAcceptedResponse {
  groupId: string;
  address: string;
  chainIds: number[];
}

export interface ChainCandidatesResponse {
  address: string;
  chainIds: number[];
}

export interface ScanGroupChainStatus {
  chainId: number;
  scanId: string;
  status: ScanStage;
}

export interface ScanGroupView {
  groupId: string;
  address: string;
  completed: boolean;
  chains: ScanGroupChainStatus[];
}

export interface TokenBalance {
  symbol: string;
  balanceFormatted: string;
  usdValue: number | null;
}

export interface ScanReportView {
  scanId: string;
  address: string;
  chainId: number;
  riskLevel: RiskLevel;
  score: number;
  explanation: string;
  decisiveSignals: string[];
  manualChecks: string[];
  balanceWei: string;
  txCount: number;
  txCount24h: number;
  sampleTruncated: boolean;
  observedAt: string;
  tokenBalances: TokenBalance[];
  model: string;
  createdAt: string;
}

export interface ScanGroupReportView {
  groupId: string;
  address: string;
  reports: ScanReportView[];
}

export interface ScanProgressMessage {
  scanId: string;
  stage: ScanStage;
  message: string;
  at: string;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
