import type { RiskLevel } from "@/lib/types";

export const RISK: Record<RiskLevel, { text: string; border: string; bg: string }> = {
  LOW: { text: "text-risk-low", border: "border-risk-low", bg: "bg-risk-low" },
  MEDIUM: { text: "text-risk-mid", border: "border-risk-mid", bg: "bg-risk-mid" },
  HIGH: { text: "text-risk-high", border: "border-risk-high", bg: "bg-risk-high" },
  CRITICAL: { text: "text-risk-critical", border: "border-risk-critical", bg: "bg-risk-critical" },
};

export const RISK_ORDER: RiskLevel[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export function riskAccentClass(level: RiskLevel): string {
  return RISK[level].text;
}

export function riskBgClass(level: RiskLevel): string {
  return RISK[level].bg;
}
