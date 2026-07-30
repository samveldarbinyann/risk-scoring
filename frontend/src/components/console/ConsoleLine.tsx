import { motion } from "motion/react";
import type { ScanStage } from "@/lib/types";
import { formatTime } from "@/lib/format";
import { cn } from "@/lib/cn";
import { TypewriterText } from "@/components/ui/TypewriterText";
import { STAGE_TONE } from "@/components/console/StageCopy";

interface ConsoleLineProps {
  stage: ScanStage;
  message: string;
  at: string;
  chain?: string;
}

export function ConsoleLine({ stage, message, at, chain }: ConsoleLineProps) {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: "easeOut" }}
      className="flex shrink-0 items-baseline gap-3 font-mono text-sm"
    >
      <span className="shrink-0 text-text-faint">{formatTime(at)}</span>
      {chain && <span className="shrink-0 text-xs text-text-dim">[{chain}]</span>}
      <span className={cn("shrink-0 text-xs uppercase tracking-wider", STAGE_TONE[stage])}>{stage}</span>
      <TypewriterText as="span" text={message} className="text-text" />
    </motion.div>
  );
}
