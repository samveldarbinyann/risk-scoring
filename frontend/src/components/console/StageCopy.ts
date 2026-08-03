import type { ScanStage } from "@/lib/types";
import type { MessageKey } from "@/lib/i18n/messageKeys";

export const STAGE_TONE: Record<ScanStage, string> = {
  PENDING: "text-text-dim",
  FETCHING: "text-accent",
  ENRICHING: "text-accent",
  ANALYZING: "text-accent",
  COMPLETED: "text-risk-low",
  FAILED: "text-risk-critical",
};

export const STAGE_CODENAME: Record<ScanStage, MessageKey> = {
  PENDING: "console.stage.PENDING",
  FETCHING: "console.stage.FETCHING",
  ENRICHING: "console.stage.ENRICHING",
  ANALYZING: "console.stage.ANALYZING",
  COMPLETED: "console.stage.COMPLETED",
  FAILED: "console.stage.FAILED",
};
