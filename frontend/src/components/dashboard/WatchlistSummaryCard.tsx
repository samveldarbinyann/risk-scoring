import { useNavigate } from "react-router";
import { Sparkline } from "@/components/dashboard/Sparkline";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
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
  const navigate = useNavigate();

  const neverChecked = entries.filter((entry) => !entry.lastCheckedAt).length;
  const trend = bucketCumulativeCounts(
    entries.map((entry) => entry.createdAt),
    TREND_DAYS,
  );
  const { start: windowStart, end: now } = trendWindow(TREND_DAYS);

  return (
    <Card title={t("dashboard.watchlist.title")} className="flex flex-col gap-4">
      <CardState isLoading={isLoading} error={error}>
        <Sparkline
          values={trend}
          startLabel={formatShortDate(windowStart.toISOString(), locale)}
          endLabel={formatShortDate(now.toISOString(), locale)}
        />

        {entries.length === 0 ? (
          <p className="font-mono text-sm text-text-faint">{t("dashboard.watchlist.empty")}</p>
        ) : (
          <p className="font-mono text-xs text-text-faint">
            {t("dashboard.watchlist.neverChecked")}: {formatCount(neverChecked, locale)}
          </p>
        )}

        <Button type="button" variant="ghost" onClick={() => navigate("/watchlist")} className="w-fit">
          {t("dashboard.watchlist.cta")}
        </Button>
      </CardState>
    </Card>
  );
}
