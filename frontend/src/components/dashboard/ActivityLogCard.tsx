import { useNavigate } from "react-router";
import { ActivityLogRow, type ActivityEvent } from "@/components/dashboard/ActivityLogRow";
import { Sparkline } from "@/components/dashboard/Sparkline";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import { TypewriterCaret } from "@/components/ui/TypewriterCaret";
import { bucketDailyCounts } from "@/lib/dashboardStats";
import { formatShortDate } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { AlertView, RecentScanGroupView } from "@/lib/types";

const MAX_VISIBLE_EVENTS = 8;
const TREND_DAYS = 7;

interface ActivityLogCardProps {
  scans: RecentScanGroupView[];
  alerts: AlertView[];
  isLoading: boolean;
  error: string | null;
}

export function ActivityLogCard({ scans, alerts, isLoading, error }: ActivityLogCardProps) {
  const { t, locale } = useI18n();
  const navigate = useNavigate();

  const events: ActivityEvent[] = [
    ...scans.map((scan) => ({ kind: "scan" as const, at: scan.requestedAt, scan })),
    ...alerts.map((alert) => ({ kind: "alert" as const, at: alert.triggeredAt, alert })),
  ].sort((a, b) => new Date(b.at).getTime() - new Date(a.at).getTime());

  const visibleEvents = events.slice(0, MAX_VISIBLE_EVENTS);

  const trend = bucketDailyCounts(
    events.map((event) => event.at),
    TREND_DAYS,
  );
  const now = new Date();
  const windowStart = new Date(now);
  windowStart.setDate(windowStart.getDate() - (TREND_DAYS - 1));

  return (
    <Card className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <h3 className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("dashboard.activity.title")}</h3>
        <Sparkline
          values={trend}
          startLabel={formatShortDate(windowStart.toISOString(), locale)}
          endLabel={formatShortDate(now.toISOString(), locale)}
          className="w-28 sm:w-40"
        />
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <Spinner />
        </div>
      ) : error ? (
        <ErrorMessage message={error} size="sm" />
      ) : visibleEvents.length === 0 ? (
        <p className="font-mono text-sm text-text-faint">
          &gt; {t("dashboard.activity.waiting")}
          <TypewriterCaret />
        </p>
      ) : (
        <>
          <div>
            {visibleEvents.map((event) => (
              <ActivityLogRow
                key={event.kind === "scan" ? `scan-${event.scan.groupId}` : `alert-${event.alert.id}`}
                event={event}
              />
            ))}
          </div>
          <Button type="button" variant="ghost" onClick={() => navigate("/alerts")} className="w-fit">
            {t("dashboard.alerts.cta")}
          </Button>
        </>
      )}
    </Card>
  );
}
