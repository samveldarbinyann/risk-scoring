import type { RiskLevel } from "@/lib/types";
import { RISK } from "@/lib/risk";
import { cn } from "@/lib/cn";

interface RiskBadgeProps {
  level: RiskLevel;
  className?: string;
}

export function RiskBadge({ level, className }: RiskBadgeProps) {
  const risk = RISK[level];
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-base border px-2.5 py-1 font-mono text-xs",
        risk.text,
        risk.border,
        className,
      )}
    >
      {risk.label}
    </span>
  );
}
