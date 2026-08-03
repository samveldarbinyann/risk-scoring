import type { MessageKey } from "@/lib/i18n/messageKeys";
import type { PlanCode } from "@/lib/types";

export const PLAN_ORDER: PlanCode[] = ["FREE", "STARTER", "GROWTH", "SCALE"];

export const POPULAR_PLAN: PlanCode = "GROWTH";

/** Shared inclusion bullets; monthly quota is rendered from API limit separately. */
export const PLAN_FEATURE_KEYS: MessageKey[] = [
  "pricing.feature.apiAccess",
  "pricing.feature.llmVerdict",
  "pricing.feature.multiEvm",
];
