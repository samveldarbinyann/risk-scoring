import type { ScanStage } from "@/lib/types";

export const STAGE_TONE: Record<ScanStage, string> = {
  PENDING: "text-text-dim",
  FETCHING: "text-accent",
  ENRICHING: "text-accent",
  ANALYZING: "text-accent",
  COMPLETED: "text-risk-low",
  FAILED: "text-risk-critical",
};
