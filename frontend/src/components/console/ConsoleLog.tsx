import { useEffect, useRef } from "react";
import { AnimatePresence } from "motion/react";
import type { ScanProgressMessage } from "@/lib/types";
import { ConsoleLine } from "@/components/console/ConsoleLine";
import { chainLabel } from "@/lib/chains";

interface ConsoleLogProps {
  lines: ScanProgressMessage[];
  chainByScanId?: Map<string, number>;
}

export function ConsoleLog({ lines, chainByScanId }: ConsoleLogProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [lines.length]);

  return (
    <div className="flex flex-col gap-2">
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
      <div ref={bottomRef} />
    </div>
  );
}
