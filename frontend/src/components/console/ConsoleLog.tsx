import { AnimatePresence } from "motion/react";
import type { ScanProgressMessage } from "@/lib/types";
import { ConsoleLine } from "@/components/console/ConsoleLine";
import { chainLabel } from "@/lib/chains";
import { useI18n } from "@/lib/i18n/context";

interface ConsoleLogProps {
  lines: ScanProgressMessage[];
  chainByScanId?: Map<string, number>;
}

export function ConsoleLog({ lines, chainByScanId }: ConsoleLogProps) {
  const { t } = useI18n();

  return (
    <div className="flex h-40 w-full max-w-2xl flex-col justify-end gap-2 overflow-hidden rounded-panel border border-border bg-surface p-4">
      {lines.length === 0 ? (
        <p className="font-mono text-sm text-text-faint">{t("console.connecting")}</p>
      ) : (
        <AnimatePresence initial={false}>
          {lines.map((line, index) => {
            const chainId = chainByScanId?.get(line.scanId);
            return (
              <ConsoleLine
                key={`${line.scanId}-${line.stage}-${index}`}
                stage={line.stage}
                message={line.message}
                at={line.at}
                chain={chainId !== undefined ? chainLabel(chainId) : undefined}
              />
            );
          })}
        </AnimatePresence>
      )}
    </div>
  );
}
