export type ScanStage = "PENDING" | "FETCHING" | "ENRICHING" | "ANALYZING" | "COMPLETED" | "FAILED";
export type ScanSource = "USER" | "MONITOR";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface ScanCreateRequest {
  address: string;
}

export interface ScanGroupAcceptedResponse {
  groupId: string;
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
