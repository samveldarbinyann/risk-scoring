import { TargetChip } from "@/components/ui/TargetChip";
import { Button } from "@/components/ui/Button";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { chainLabel } from "@/lib/chains";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { WatchlistEntryView } from "@/lib/types";

interface WatchlistEntryRowProps {
  entry: WatchlistEntryView;
  isRemoving: boolean;
  onRemove: (id: string) => void;
}

export function WatchlistEntryRow({ entry, isRemoving, onRemove }: WatchlistEntryRowProps) {
  const { t, locale } = useI18n();

  return (
    <div className="flex flex-col gap-3 border-b border-border py-4 last:border-b-0 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-center gap-3">
          <ChainIcon chainId={entry.chainId} className="h-5 w-5 shrink-0 text-text-dim" />
          <span className="font-sans text-sm text-text">{chainLabel(entry.chainId)}</span>
          <TargetChip value={entry.address} className="min-w-0 text-sm" />
        </div>
        <div className="flex flex-wrap items-center gap-3 pl-8">
          {entry.lastRiskLevel ? (
            <RiskBadge level={entry.lastRiskLevel} />
          ) : (
            <span className="font-mono text-xs text-text-faint">—</span>
          )}
          <span className="font-mono text-xs text-text-dim">
            {t("watchlist.score")}: {entry.lastScore ?? "—"}
          </span>
          <span className="font-mono text-xs text-text-faint">
            {t("watchlist.lastChecked")}:{" "}
            {entry.lastCheckedAt ? formatDateTime(entry.lastCheckedAt, locale) : t("watchlist.neverChecked")}
          </span>
        </div>
      </div>
      <Button
        type="button"
        variant="ghost"
        isLoading={isRemoving}
        onClick={() => onRemove(entry.id)}
        className="h-10 shrink-0 px-4 text-sm sm:self-center"
      >
        {t("watchlist.remove")}
      </Button>
    </div>
  );
}
