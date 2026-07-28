import { motion } from "motion/react";
import type { RiskLevel } from "@/lib/types";
import { riskAccentClass } from "@/lib/risk";
import { ScoreCounter } from "@/components/report/ScoreCounter";
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
      className="flex items-center justify-between gap-6 rounded-panel border border-border bg-surface p-8"
    >
      <div>
        <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("report.verdict")}</p>
        <p className={cn("font-sans text-4xl font-semibold", riskAccentClass(level))}>{level}</p>
      </div>
      <ScoreCounter score={score} />
    </motion.div>
  );
}
