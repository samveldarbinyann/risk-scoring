import { motion } from "motion/react";
import type { RiskLevel } from "@/lib/types";
import { riskAccentClass } from "@/lib/risk";
import { ScoreCounter } from "@/components/report/ScoreCounter";
import { ScoreMeter } from "@/components/report/ScoreMeter";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";

interface VerdictRevealProps {
  level: RiskLevel;
  score: number;
}

export function VerdictReveal({ level, score }: VerdictRevealProps) {
  const { t } = useI18n();

  return (
    <motion.div
      initial={{ opacity: 0, x: -16 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      className="flex flex-col gap-6 p-6 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("report.verdict")}</p>
        <p className={cn("font-sans text-balance text-4xl font-semibold", riskAccentClass(level))}>
          {t(`risk.level.${level}` as MessageKey)}
        </p>
      </div>
      <div className="flex flex-col items-start gap-2 sm:w-40 sm:items-end">
        <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("report.score")}</p>
        <ScoreCounter score={score} />
        <ScoreMeter level={level} score={score} />
      </div>
    </motion.div>
  );
}
