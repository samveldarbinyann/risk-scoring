import { motion } from "motion/react";
import type { RiskLevel } from "@/lib/types";
import { riskAccentClass } from "@/lib/risk";
import { ScoreCounter } from "@/components/report/ScoreCounter";
import { ScoreMeter } from "@/components/report/ScoreMeter";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";

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
      className="flex items-center justify-between gap-6 p-6"
    >
      <div>
        <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("report.verdict")}</p>
        <p className={cn("font-sans text-4xl font-semibold", riskAccentClass(level))}>{level}</p>
      </div>
      <div className="flex w-40 flex-col items-end gap-2">
        <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("report.score")}</p>
        <ScoreCounter score={score} />
        <ScoreMeter level={level} score={score} />
      </div>
    </motion.div>
  );
}
