import { useNavigate } from "react-router";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { useCountUp } from "@/hooks/useCountUp";
import { cn } from "@/lib/cn";
import { averageScore, dominantRiskLevel } from "@/lib/dashboardStats";
import { formatCount, UNKNOWN_VALUE } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { RISK, RISK_ORDER } from "@/lib/risk";
import type { WatchlistEntryView } from "@/lib/types";

interface PortfolioHeroCardProps {
  entries: WatchlistEntryView[];
  isLoading: boolean;
  error: string | null;
}

export function PortfolioHeroCard({ entries, isLoading, error }: PortfolioHeroCardProps) {
  const { t, locale } = useI18n();
  const navigate = useNavigate();

  const total = entries.length;
  const avgScoreValue = averageScore(entries);
  const dominant = dominantRiskLevel(entries);
  const animatedTotal = useCountUp(total);
  const animatedAvgScore = useCountUp(avgScoreValue ?? 0);

  const counts = RISK_ORDER.map((level) => ({
    level,
    count: entries.filter((entry) => entry.lastRiskLevel === level).length,
  }));

  return (
    <Card className="flex flex-col gap-6">
      <CardState isLoading={isLoading} error={error}>
        <>
          <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("dashboard.hero.eyebrow")}</p>
              <p className="font-mono text-5xl font-semibold tabular-nums text-text sm:text-6xl">
                {formatCount(animatedTotal, locale)}
              </p>
              <p className="font-mono text-sm text-text-dim">{t("dashboard.watchlist.active")}</p>
            </div>

            <div className="flex gap-8">
              <div>
                <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("dashboard.hero.avgScore")}</p>
                <p className="font-mono text-2xl font-semibold tabular-nums text-text">
                  {avgScoreValue === null ? UNKNOWN_VALUE : formatCount(animatedAvgScore, locale)}
                  {avgScoreValue !== null && (
                    <span className="ml-1 text-sm font-normal text-text-faint">{t("report.scoreSuffix")}</span>
                  )}
                </p>
              </div>

              {dominant && (
                <div>
                  <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("dashboard.hero.dominant")}</p>
                  <div className="mt-2">
                    <RiskBadge level={dominant} />
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <div className="flex h-2 overflow-hidden rounded-base bg-surface-2">
              {counts.map(({ level, count }) =>
                count === 0 ? null : (
                  <div key={level} className={cn("h-full", RISK[level].bg)} style={{ flexGrow: count, flexBasis: 0 }} />
                ),
              )}
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1 font-mono text-xs text-text-dim">
              {counts.map(({ level, count }) => (
                <span key={level} className={RISK[level].text}>
                  {t(`risk.level.${level}` as MessageKey)}: {formatCount(count, locale)}
                </span>
              ))}
            </div>
          </div>

          <Button type="button" variant="ghost" onClick={() => navigate("/watchlist")} className="w-fit">
            {total === 0 ? t("dashboard.hero.emptyCta") : t("dashboard.watchlist.cta")}
          </Button>
        </>
      </CardState>
    </Card>
  );
}
