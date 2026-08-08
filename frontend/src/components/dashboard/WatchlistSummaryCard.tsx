import { Sparkline } from "@/components/dashboard/Sparkline";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { LinkButton } from "@/components/ui/LinkButton";
import { bucketCumulativeCounts, trendWindow } from "@/lib/dashboardStats";
import { formatCount, formatShortDate } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { WatchlistEntryView } from "@/lib/types";

const TREND_DAYS = 30;

interface WatchlistSummaryCardProps {
  entries: WatchlistEntryView[];
  isLoading: boolean;
  error: string | null;
}

export function WatchlistSummaryCard({ entries, isLoading, error }: WatchlistSummaryCardProps) {
  const { t, locale } = useI18n();

  const neverChecked = entries.filter((entry) => !entry.lastCheckedAt).length;
  const trend = bucketCumulativeCounts(
    entries.map((entry) => entry.createdAt),
    TREND_DAYS,
  );
  const { start: windowStart, end: now } = trendWindow(TREND_DAYS);

  return (
    <Card title={t("dashboard.watchlist.title")} className="flex h-full flex-col gap-4">
      <CardState isLoading={isLoading} error={error}>
        <Sparkline
          values={trend}
          startLabel={formatShortDate(windowStart.toISOString(), locale)}
          endLabel={formatShortDate(now.toISOString(), locale)}
        />

        {entries.length === 0 ? (
          <EmptyState message={t("dashboard.watchlist.empty")} />
        ) : (
          <p className="font-mono text-xs text-text-faint">
            {t("dashboard.watchlist.neverChecked")}: {formatCount(neverChecked, locale)}
          </p>
        )}

        <LinkButton to="/watchlist" variant="ghost" className="mt-auto w-fit">
          {t("dashboard.watchlist.cta")}
        </LinkButton>
      </CardState>
    </Card>
  );
}
