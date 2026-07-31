import type { RiskLevel } from "@/lib/types";

export const RISK: Record<RiskLevel, { label: string; text: string; border: string }> = {
  LOW: { label: "LOW", text: "text-risk-low", border: "border-risk-low" },
  MEDIUM: { label: "MEDIUM", text: "text-risk-mid", border: "border-risk-mid" },
  HIGH: { label: "HIGH", text: "text-risk-high", border: "border-risk-high" },
  CRITICAL: { label: "CRITICAL", text: "text-risk-critical", border: "border-risk-critical" },
};

export const RISK_ORDER: RiskLevel[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export function riskAccentClass(level: RiskLevel): string {
  return RISK[level].text;
}
