import type { RiskLevel } from "@/lib/types";
import { RISK } from "@/lib/risk";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";

interface RiskBadgeProps {
  level: RiskLevel;
  className?: string;
  bare?: boolean;
}

export function RiskBadge({ level, className, bare = false }: RiskBadgeProps) {
  const { t } = useI18n();
  const risk = RISK[level];
  return (
    <span
      className={cn(
        "inline-flex items-center font-mono text-xs",
        bare ? risk.text : cn("rounded-base border px-2.5 py-1", risk.text, risk.border),
        className,
      )}
    >
      {t(`risk.level.${level}` as MessageKey)}
    </span>
  );
}
