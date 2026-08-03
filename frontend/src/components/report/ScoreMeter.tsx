import { motion } from "motion/react";
import type { RiskLevel } from "@/lib/types";
import { riskBgClass } from "@/lib/risk";
import { cn } from "@/lib/cn";

interface ScoreMeterProps {
  level: RiskLevel;
  score: number;
}

export function ScoreMeter({ level, score }: ScoreMeterProps) {
  const ratio = Math.min(1, Math.max(0, score / 100));

  return (
    <div className="h-2 w-full overflow-hidden rounded-base bg-surface-2">
      <motion.div
        initial={{ scaleX: 0 }}
        animate={{ scaleX: ratio }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        className={cn("h-full origin-left rounded-base", riskBgClass(level))}
      />
    </div>
  );
}
