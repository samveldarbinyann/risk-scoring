import { motion } from "motion/react";
import type { ScanStage } from "@/lib/types";
import { formatTime } from "@/lib/format";
import { cn } from "@/lib/cn";
import { useTypewriter } from "@/hooks/useTypewriter";
import { STAGE_TONE } from "@/components/console/StageCopy";

interface ConsoleLineProps {
  stage: ScanStage;
  message: string;
  at: string;
  chain?: string;
}

export function ConsoleLine({ stage, message, at, chain }: ConsoleLineProps) {
  const { text: displayed, isTyping } = useTypewriter(message);

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
      <span className="text-text">
        {displayed}
        {isTyping && (
          <motion.span
            aria-hidden
            className="ml-0.5 inline-block h-[0.9em] w-[0.5em] translate-y-[0.1em] bg-current align-baseline"
            animate={{ opacity: [1, 1, 0, 0] }}
            transition={{ duration: 0.9, repeat: Infinity, ease: "linear", times: [0, 0.5, 0.5, 1] }}
          />
        )}
      </span>
    </motion.div>
  );
}
