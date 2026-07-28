import { useEffect, useRef } from "react";
import { AnimatePresence } from "motion/react";
import type { ScanProgressMessage } from "@/lib/types";
import { ConsoleLine } from "@/components/console/ConsoleLine";

interface ConsoleLogProps {
  lines: ScanProgressMessage[];
}

export function ConsoleLog({ lines }: ConsoleLogProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [lines.length]);

  return (
    <div className="flex flex-col gap-2">
      <AnimatePresence initial={false}>
        {lines.map((line, index) => (
          <ConsoleLine key={`${line.stage}-${index}`} stage={line.stage} message={line.message} at={line.at} />
        ))}
      </AnimatePresence>
      <div ref={bottomRef} />
    </div>
  );
}
