import { motion } from "motion/react";
import type { ScanStage } from "@/lib/types";
import { formatTime } from "@/lib/format";
import { TypewriterText } from "@/components/ui/TypewriterText";
import { STAGE_CODENAME, STAGE_TONE } from "@/components/console/StageCopy";
import { useI18n } from "@/lib/i18n/context";

interface ConsoleLineProps {
  stage: ScanStage;
  message: string;
  at: string;
  chain?: string;
  msPerChar?: number;
}

export function ConsoleLine({ stage, message, at, chain, msPerChar }: ConsoleLineProps) {
  const { locale, t } = useI18n();

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: "easeOut" }}
      className="flex shrink-0 flex-col items-start gap-1 font-mono text-sm sm:flex-row sm:items-baseline sm:gap-3"
    >
      <div className="flex flex-wrap items-baseline gap-3">
        <span className="shrink-0 text-accent">&gt;</span>
        <span className="shrink-0 text-text-faint">{formatTime(at, locale)}</span>
        {chain && <span className="shrink-0 text-xs text-text-dim">[{chain}]</span>}
        <span className="shrink-0 text-xs uppercase tracking-wider">
          <span className="text-text-faint">[</span>
          <span className={STAGE_TONE[stage]}>{t(STAGE_CODENAME[stage])}</span>
          <span className="text-text-faint">]</span>
        </span>
      </div>
      <TypewriterText as="span" text={message} msPerChar={msPerChar} className="w-full text-text sm:w-auto" />
    </motion.div>
  );
}
