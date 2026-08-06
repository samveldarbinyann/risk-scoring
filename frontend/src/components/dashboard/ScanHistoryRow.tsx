import { useNavigate } from "react-router";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { TargetChip } from "@/components/ui/TargetChip";
import { useChains } from "@/lib/chains/context";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import { SCAN_SOURCE } from "@/lib/scanSource";
import type { RecentScanGroupView } from "@/lib/types";

interface ScanHistoryRowProps {
  scan: RecentScanGroupView;
}

export function ScanHistoryRow({ scan }: ScanHistoryRowProps) {
  const { t, locale } = useI18n();
  const { label } = useChains();
  const navigate = useNavigate();

  const path = scan.completed ? `/scan/${scan.groupId}/report` : `/scan/${scan.groupId}`;

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => navigate(path)}
      onKeyDown={(keyEvent) => {
        if (keyEvent.key === "Enter" || keyEvent.key === " ") navigate(path);
      }}
      className="flex w-full cursor-pointer flex-col gap-2 border-b border-border py-3 text-left last:border-b-0 sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="flex min-w-0 items-center gap-3">
        <span className="shrink-0 font-mono text-xs text-text-faint">{formatDateTime(scan.requestedAt, locale)}</span>
        <span className="shrink-0 rounded-base border border-border px-2 py-0.5 font-mono text-xs uppercase tracking-wider text-text-faint">
          {t(SCAN_SOURCE[scan.source].labelKey)}
        </span>

        <div className="flex shrink-0 -space-x-1.5">
          {scan.chains.map((chain) => (
            <span key={chain} title={label(chain)}>
              <ChainIcon chain={chain} className="h-4 w-4 rounded-full bg-surface text-text-dim" />
            </span>
          ))}
        </div>
        <span onClick={(clickEvent) => clickEvent.stopPropagation()}>
          <TargetChip value={scan.target} className="min-w-0 text-sm" />
        </span>
      </div>

      <div className="flex items-center gap-2 pl-8 sm:pl-0">
        {scan.worstRiskLevel ? (
          <RiskBadge level={scan.worstRiskLevel} />
        ) : (
          <span className="font-mono text-xs text-text-faint">{t("dashboard.recentScans.inProgress")}</span>
        )}
      </div>
    </div>
  );
}
