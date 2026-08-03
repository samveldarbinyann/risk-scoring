import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import type { ScanProgressMessage } from "@/lib/types";
import { ConsoleLine } from "@/components/console/ConsoleLine";
import { useChains } from "@/lib/chains/context";
import type { Chain } from "@/lib/chains/registry";
import { typewriterDurationMs } from "@/hooks/useTypewriter";
import { useI18n } from "@/lib/i18n/context";

const CONSOLE_MS_PER_CHAR = 28;
const LINE_GAP_MS = 160;

interface ConsoleLogProps {
  lines: ScanProgressMessage[];
  chainByScanId?: Map<string, Chain>;
  completed: boolean;
  onPlaybackChange?: (visibleLines: ScanProgressMessage[]) => void;
  onPlaybackComplete?: () => void;
}

export function ConsoleLog({ lines, chainByScanId, completed, onPlaybackChange, onPlaybackComplete }: ConsoleLogProps) {
  const { t } = useI18n();
  const { label } = useChains();
  const [visibleLineCount, setVisibleLineCount] = useState(0);

  useEffect(() => {
    if (lines.length === 0) {
      setVisibleLineCount(0);
      return;
    }

    if (visibleLineCount > lines.length) {
      setVisibleLineCount(lines.length);
      return;
    }

    if (visibleLineCount === 0) {
      setVisibleLineCount(1);
      return;
    }

    if (visibleLineCount === lines.length) return;

    const previousLine = lines[visibleLineCount - 1];
    const timeout = setTimeout(
      () => setVisibleLineCount((count) => Math.min(count + 1, lines.length)),
      typewriterDurationMs(previousLine.message, CONSOLE_MS_PER_CHAR) + LINE_GAP_MS,
    );
    return () => clearTimeout(timeout);
  }, [lines, visibleLineCount]);

  const visibleLines = lines.slice(0, visibleLineCount);

  useEffect(() => {
    onPlaybackChange?.(lines.slice(0, visibleLineCount));
  }, [lines, onPlaybackChange, visibleLineCount]);

  useEffect(() => {
    if (!completed || visibleLineCount !== lines.length || lines.length === 0) return;

    const lastLine = lines[lines.length - 1];
    const timeout = setTimeout(
      () => onPlaybackComplete?.(),
      typewriterDurationMs(lastLine.message, CONSOLE_MS_PER_CHAR) + LINE_GAP_MS,
    );
    return () => clearTimeout(timeout);
  }, [completed, lines, onPlaybackComplete, visibleLineCount]);

  return (
    <motion.section
      initial={{ opacity: 0, scaleY: 0.7 }}
      animate={{ opacity: 1, scaleY: 1 }}
      transition={{ opacity: { duration: 0.2 }, scaleY: { duration: 0.2 } }}
      className="w-full origin-top"
    >
      <header className="flex items-center justify-between border-b border-border bg-surface-2 px-4 py-3">
        <div className="flex items-center gap-3">
          <motion.span
            animate={{ opacity: [0.5, 1, 0.5] }}
            transition={{ duration: 1.2, ease: "easeInOut", repeat: Infinity }}
            className="h-2 w-2 rounded-base bg-accent"
          />
          <h2 className="font-mono text-xs uppercase tracking-widest text-text-dim">{t("console.eventStream")}</h2>
        </div>
        <span className="font-mono text-xs text-text-faint">{String(visibleLines.length).padStart(2, "0")}</span>
      </header>
      <div className="flex min-h-16 max-h-96 flex-col gap-2 overflow-y-auto p-4">
        {visibleLines.length === 0 ? (
          <p className="font-mono text-sm text-text-faint">&gt; {t("console.waiting")}</p>
        ) : (
          <AnimatePresence initial={false}>
            {visibleLines.map((line, index) => {
              const chain = chainByScanId?.get(line.scanId);
              return (
                <ConsoleLine
                  key={`${line.scanId}-${line.stage}-${index}`}
                  stage={line.stage}
                  message={line.message}
                  at={line.at}
                  chain={chain !== undefined ? label(chain) : undefined}
                  msPerChar={CONSOLE_MS_PER_CHAR}
                />
              );
            })}
          </AnimatePresence>
        )}
      </div>
    </motion.section>
  );
}
