import { motion } from "motion/react";
import type { ScanStage } from "@/lib/types";
import { formatTime } from "@/lib/format";
import { cn } from "@/lib/cn";
import { STAGE_TONE } from "@/components/console/StageCopy";

interface ConsoleLineProps {
  stage: ScanStage;
  message: string;
  at: string;
}

export function ConsoleLine({ stage, message, at }: ConsoleLineProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: "easeOut" }}
      className="flex items-baseline gap-3 font-mono text-sm"
    >
      <span className="shrink-0 text-text-faint">{formatTime(at)}</span>
      <span className={cn("shrink-0 text-xs uppercase tracking-wider", STAGE_TONE[stage])}>{stage}</span>
      <span className="text-text">{message}</span>
    </motion.div>
  );
}
