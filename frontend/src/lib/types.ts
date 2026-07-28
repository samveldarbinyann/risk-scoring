export type ScanStage = "PENDING" | "FETCHING" | "ENRICHING" | "ANALYZING" | "COMPLETED" | "FAILED";
export type ScanSource = "USER" | "MONITOR";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface ScanCreateRequest {
  address: string;
  chainId: number;
}

export interface ScanAcceptedResponse {
  scanId: string;
  status: ScanStage;
}

export interface ScanView {
  scanId: string;
  address: string;
  chainId: number;
  status: ScanStage;
  source: ScanSource;
  requestedAt: string;
  completedAt: string | null;
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
  model: string;
  createdAt: string;
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
