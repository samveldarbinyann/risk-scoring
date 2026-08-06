import { TargetChip } from "@/components/ui/TargetChip";
import { Button } from "@/components/ui/Button";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { useChains } from "@/lib/chains/context";
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
  const { label } = useChains();

  return (
    <div className="flex flex-col gap-3 border-b border-border px-3 py-4 transition-colors last:border-b-0 hover:bg-surface-2 sm:flex-row sm:items-center sm:gap-4">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <ChainIcon chain={entry.chain} className="h-5 w-5 shrink-0 text-text-dim" />
        <span className="shrink-0 font-sans text-sm text-text-dim">{label(entry.chain)}</span>
        <TargetChip value={entry.address} className="min-w-0 text-sm" />
      </div>

      <div className="flex flex-wrap items-center gap-4 pl-8 sm:shrink-0 sm:pl-0">
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

      <Button
        type="button"
        variant="ghost"
        isLoading={isRemoving}
        onClick={() => onRemove(entry.id)}
        className="h-9 shrink-0 self-start px-4 text-sm sm:self-center"
      >
        {t("watchlist.remove")}
      </Button>
    </div>
  );
}
