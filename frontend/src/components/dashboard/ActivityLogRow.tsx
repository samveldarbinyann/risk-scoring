import { useNavigate } from "react-router";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { TargetChip } from "@/components/ui/TargetChip";
import { useChains } from "@/lib/chains/context";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { AlertView, RecentScanGroupView } from "@/lib/types";

export type ActivityEvent =
  | { kind: "scan"; at: string; scan: RecentScanGroupView }
  | { kind: "alert"; at: string; alert: AlertView };

interface ActivityLogRowProps {
  event: ActivityEvent;
}

export function ActivityLogRow({ event }: ActivityLogRowProps) {
  const { t, locale } = useI18n();
  const { label } = useChains();
  const navigate = useNavigate();

  const path =
    event.kind === "scan"
      ? event.scan.completed
        ? `/scan/${event.scan.groupId}/report`
        : `/scan/${event.scan.groupId}`
      : "/alerts";

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
        <span className="shrink-0 font-mono text-xs text-text-faint">{formatDateTime(event.at, locale)}</span>
        <span className="shrink-0 rounded-base border border-border px-2 py-0.5 font-mono text-xs uppercase tracking-wider text-text-faint">
          {event.kind === "scan" ? t("dashboard.activity.scanTag") : t("dashboard.activity.alertTag")}
        </span>

        {event.kind === "scan" ? (
          <>
            <div className="flex shrink-0 -space-x-1.5">
              {event.scan.chains.map((chain) => (
                <span key={chain} title={label(chain)}>
                  <ChainIcon chain={chain} className="h-4 w-4 rounded-full bg-surface text-text-dim" />
                </span>
              ))}
            </div>
            <span onClick={(clickEvent) => clickEvent.stopPropagation()}>
              <TargetChip value={event.scan.target} className="min-w-0 text-sm" />
            </span>
          </>
        ) : (
          <>
            <span title={label(event.alert.chain)}>
              <ChainIcon chain={event.alert.chain} className="h-4 w-4 shrink-0 text-text-dim" />
            </span>
            <span onClick={(clickEvent) => clickEvent.stopPropagation()}>
              <TargetChip value={event.alert.address} className="min-w-0 text-sm" />
            </span>
          </>
        )}
      </div>

      <div className="flex items-center gap-2 pl-8 sm:pl-0">
        {event.kind === "scan" ? (
          event.scan.worstRiskLevel ? (
            <RiskBadge level={event.scan.worstRiskLevel} />
          ) : (
            <span className="font-mono text-xs text-text-faint">{t("dashboard.recentScans.inProgress")}</span>
          )
        ) : (
          <>
            <RiskBadge level={event.alert.previousRiskLevel} />
            <span className="font-mono text-xs text-text-faint">&rarr;</span>
            <RiskBadge level={event.alert.newRiskLevel} />
          </>
        )}
      </div>
    </div>
  );
}
