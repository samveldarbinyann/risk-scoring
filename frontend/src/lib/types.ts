import type { Chain } from "@/lib/chains/registry";

export type ScanStage = "PENDING" | "FETCHING" | "ENRICHING" | "ANALYZING" | "COMPLETED" | "FAILED";
export type ScanSource = "USER" | "MONITOR" | "API";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type PlanCode = "FREE" | "STARTER" | "GROWTH" | "SCALE";
export type SubscriptionStatus = "PENDING_PAYMENT" | "ACTIVE" | "CANCELED" | "EXPIRED";
export type ApiKeyStatus = "ACTIVE" | "REVOKED";

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

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export type ScanTarget = "ADDRESS" | "TRANSACTION";

export type LabelCategory = "SANCTION" | "MIXER" | "EXCHANGE";

export type TransferDirection = "IN" | "OUT" | "BOTH";

export type TransactionRole =
  | "SENDER"
  | "RECIPIENT"
  | "INTERNAL_SENDER"
  | "INTERNAL_RECIPIENT"
  | "TOKEN_SENDER"
  | "TOKEN_RECIPIENT";

export interface ScanCreateRequest {
  target: string;
  chains?: Chain[];
}

export interface ScanGroupAcceptedResponse {
  groupId: string;
  targetType: ScanTarget;
  target: string;
  chains: Chain[];
}

export interface ChainCandidate {
  chain: Chain;
  targetType: ScanTarget;
  normalizedTarget: string;
}

export interface ChainCandidatesResponse {
  target: string;
  candidates: ChainCandidate[];
}

export interface ScanGroupChainStatus {
  chain: Chain;
  scanId: string;
  status: ScanStage;
}

export interface ScanGroupView {
  groupId: string;
  targetType: ScanTarget;
  target: string;
  completed: boolean;
  chains: ScanGroupChainStatus[];
}

export interface TokenBalance {
  symbol: string;
  balanceFormatted: string;
  usdValue: number | null;
}

export interface TokenTransfer {
  symbol: string | null;
  contract: string;
  from: string;
  to: string;
  amount: string;
}

export interface FlaggedExposure {
  address: string;
  category: LabelCategory;
  label: string;
  source: string;
  direction: TransferDirection;
  hops: number;
  valueNative: string;
}

export interface MixerExposure {
  services: string[];
  percentOfVolume: number;
  valueNative: string;
}

export interface Heuristics {
  freshWallet: boolean | null;
  fundedThenDrained: boolean | null;
  roundAmounts: boolean;
  fanIn: number;
  fanOut: number;
}

export interface TransactionHeuristics {
  failed: boolean;
  zeroValue: boolean;
  roundValue: boolean;
  selfTransfer: boolean;
  tokenOnly: boolean;
  fanOutInternal: boolean;
  distinctPartyCount: number;
}

export interface TransactionParty {
  address: string;
  role: TransactionRole;
  valueNative: string;
}

export interface AddressEvidence {
  targetType: "ADDRESS";
  target: string;
  chain: Chain;
  observedAt: string;
  ageDays: number | null;
  txCount: number;
  txCount24h: number;
  sampleTruncated: boolean;
  balanceNative: string;
  tokenBalances: TokenBalance[];
  counterpartyCount: number;
  flagged: FlaggedExposure[];
  mixerExposure: MixerExposure | null;
  heuristics: Heuristics | null;
}

export interface TransactionEvidence {
  targetType: "TRANSACTION";
  target: string;
  chain: Chain;
  observedAt: string;
  fromAddress: string | null;
  toAddress: string | null;
  valueNative: string;
  success: boolean;
  blockTimestamp: string | null;
  nestedTransferCount: number;
  tokenTransferCount: number;
  tokenTransfers: TokenTransfer[];
  parties: TransactionParty[];
  flagged: FlaggedExposure[];
  mixerExposure: MixerExposure | null;
  heuristics: TransactionHeuristics | null;
}

export type EvidenceBundle = AddressEvidence | TransactionEvidence;

export interface ScanReportView {
  scanId: string;
  targetType: ScanTarget;
  target: string;
  chain: Chain;
  riskLevel: RiskLevel;
  score: number;
  explanation: string;
  decisiveSignals: string[];
  manualChecks: string[];
  observedAt: string;
  evidence: EvidenceBundle;
  model: string;
  createdAt: string;
}

export interface ScanGroupReportView {
  groupId: string;
  targetType: ScanTarget;
  target: string;
  reports: ScanReportView[];
}

export interface RecentScanGroupView {
  groupId: string;
  targetType: ScanTarget;
  target: string;
  chains: Chain[];
  completed: boolean;
  worstRiskLevel: RiskLevel | null;
  worstScore: number | null;
  requestedAt: string;
  source: ScanSource;
}

export interface ScanHistoryPageView {
  content: RecentScanGroupView[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
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

export interface WatchlistCreateRequest {
  address: string;
  chain: Chain;
}

export interface WatchlistEntryView {
  id: string;
  address: string;
  chain: Chain;
  lastRiskLevel: RiskLevel | null;
  lastScore: number | null;
  lastScanId: string | null;
  lastCheckedAt: string | null;
  createdAt: string;
}

export interface AlertView {
  id: string;
  watchlistEntryId: string;
  address: string;
  chain: Chain;
  previousRiskLevel: RiskLevel;
  previousScore: number;
  newRiskLevel: RiskLevel;
  newScore: number;
  scanId: string;
  triggeredAt: string;
}

export interface PlanView {
  code: PlanCode;
  priceCents: number;
  currency: string;
  monthlyRequestLimit: number;
}

export interface SubscriptionView {
  id: string;
  planCode: PlanCode;
  status: SubscriptionStatus;
  priceCents: number;
  currency: string;
  monthlyRequestLimit: number;
  requestsUsed: number;
  requestsRemaining: number;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  createdAt: string;
  canceledAt: string | null;
  paymentAddress: string | null;
  paymentAmount: number | null;
  paymentExpiresAt: string | null;
  paymentUri: string | null;
}

export interface ApiKeyView {
  id: string;
  name: string;
  keyPrefix: string;
  status: ApiKeyStatus;
  lastUsedAt: string | null;
  createdAt: string;
  revokedAt: string | null;
}

export interface ApiKeyCreatedView {
  id: string;
  name: string;
  keyPrefix: string;
  apiKey: string;
  status: ApiKeyStatus;
  createdAt: string;
}

export interface CreateApiKeyRequest {
  name: string;
}

export interface ContactRequest {
  email: string;
  subject: string;
  message: string;
  scanId: string | null;
}
