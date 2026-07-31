import { ChainIcon } from "@/components/ui/ChainIcon";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { chainLabel } from "@/lib/chains";
import { formatAddress, formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { AlertView } from "@/lib/types";

interface AlertRowProps {
  alert: AlertView;
}

export function AlertRow({ alert }: AlertRowProps) {
  const { t, locale } = useI18n();

  return (
    <div className="flex flex-col gap-3 border-b border-border py-4 last:border-b-0">
      <div className="flex flex-wrap items-center gap-3">
        <ChainIcon chainId={alert.chainId} className="h-5 w-5 shrink-0 text-text-dim" />
        <span className="font-sans text-sm text-text">{chainLabel(alert.chainId)}</span>
        <span className="font-mono text-sm text-text-dim" title={alert.address}>
          {formatAddress(alert.address)}
        </span>
        <span className="font-mono text-xs text-text-faint sm:ml-auto">
          {t("alerts.triggeredAt")}: {formatDateTime(alert.triggeredAt, locale)}
        </span>
      </div>
      <div className="flex flex-wrap items-center gap-4 pl-8">
        <div className="flex items-center gap-2">
          <span className="font-mono text-xs uppercase tracking-wider text-text-faint">{t("alerts.previous")}</span>
          <RiskBadge level={alert.previousRiskLevel} />
          <span className="font-mono text-xs text-text-dim">{alert.previousScore}</span>
        </div>
        <span className="font-mono text-xs text-text-faint">→</span>
        <div className="flex items-center gap-2">
          <span className="font-mono text-xs uppercase tracking-wider text-text-faint">{t("alerts.current")}</span>
          <RiskBadge level={alert.newRiskLevel} />
          <span className="font-mono text-xs text-text-dim">{alert.newScore}</span>
        </div>
      </div>
    </div>
  );
}
