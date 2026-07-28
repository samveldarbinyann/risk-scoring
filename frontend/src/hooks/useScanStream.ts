import { useEffect, useState } from "react";
import { subscribeScanProgress } from "@/lib/ws";
import type { ScanProgressMessage, ScanStage } from "@/lib/types";

interface ScanStreamState {
  lines: ScanProgressMessage[];
  latestStage: ScanStage | null;
}

export function useScanStream(scanId: string): ScanStreamState {
  const [lines, setLines] = useState<ScanProgressMessage[]>([]);

  useEffect(() => {
    setLines([]);
    const unsubscribe = subscribeScanProgress(scanId, (message) => {
      setLines((prev) => [...prev, message]);
    });
    return unsubscribe;
  }, [scanId]);

  return {
    lines,
    latestStage: lines.length > 0 ? lines[lines.length - 1].stage : null,
  };
}
