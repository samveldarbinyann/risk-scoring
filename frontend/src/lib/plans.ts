import type { MessageKey } from "@/lib/i18n/messageKeys";
import type { PlanCode } from "@/lib/types";

export const PLAN_ORDER: PlanCode[] = ["STARTER", "GROWTH", "SCALE"];

export const POPULAR_PLAN: PlanCode = "GROWTH";

/** Shared inclusion bullets; quota line is rendered from API limit separately. */
export const PLAN_FEATURE_KEYS: MessageKey[] = [
  "pricing.feature.chainScans",
  "pricing.feature.apiAccess",
  "pricing.feature.llmVerdict",
  "pricing.feature.multiEvm",
  "pricing.feature.apiKeys",
  "pricing.feature.watchlist",
];
