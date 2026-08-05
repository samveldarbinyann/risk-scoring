import { useNavigate } from "react-router";
import { Sparkline } from "@/components/dashboard/Sparkline";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import { bucketCumulativeCounts } from "@/lib/dashboardStats";
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
  const now = new Date();
  const windowStart = new Date(now);
  windowStart.setDate(windowStart.getDate() - (TREND_DAYS - 1));

  return (
    <Card title={t("dashboard.watchlist.title")} className="flex flex-col gap-4">
      {isLoading ? (
        <div className="flex justify-center py-8">
          <Spinner />
        </div>
      ) : error ? (
        <ErrorMessage message={error} size="sm" />
      ) : (
        <>
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
        </>
      )}
    </Card>
  );
}
