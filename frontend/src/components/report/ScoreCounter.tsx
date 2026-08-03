import { useCountUp } from "@/hooks/useCountUp";
import { useI18n } from "@/lib/i18n/context";

interface ScoreCounterProps {
  score: number;
}

export function ScoreCounter({ score }: ScoreCounterProps) {
  const { t } = useI18n();
  const value = useCountUp(score);

  return (
    <span className="font-mono text-3xl font-semibold tabular-nums text-text">
      {value}
      <span className="ml-1 text-base font-normal text-text-faint">{t("report.scoreSuffix")}</span>
    </span>
  );
}
