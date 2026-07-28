import { useEffect, useState } from "react";
import { getScanGroup } from "@/lib/api";
import { subscribeScanGroupProgress } from "@/lib/ws";
import type { ScanProgressMessage, ScanStage } from "@/lib/types";

const TERMINAL_STAGES: ScanStage[] = ["COMPLETED", "FAILED"];

interface ScanGroupStreamState {
  lines: ScanProgressMessage[];
  chainByScanId: Map<string, number>;
  completed: boolean;
}

export function useScanGroupStream(groupId: string): ScanGroupStreamState {
  const [lines, setLines] = useState<ScanProgressMessage[]>([]);
  const [chainByScanId, setChainByScanId] = useState<Map<string, number>>(new Map());
  const [expectedScanIds, setExpectedScanIds] = useState<Set<string>>(new Set());
  const [initiallyCompleted, setInitiallyCompleted] = useState(false);

  useEffect(() => {
    setLines([]);
    setChainByScanId(new Map());
    setExpectedScanIds(new Set());
    setInitiallyCompleted(false);
    if (!groupId) return;

    let cancelled = false;
    getScanGroup(groupId).then((group) => {
      if (cancelled) return;
      setChainByScanId(new Map(group.chains.map((chain) => [chain.scanId, chain.chainId])));
      setExpectedScanIds(new Set(group.chains.map((chain) => chain.scanId)));
      setInitiallyCompleted(group.completed);
    });

    const unsubscribe = subscribeScanGroupProgress(groupId, (message) => {
      setLines((prev) => [...prev, message]);
    });

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [groupId]);

  const latestStageByScanId = new Map<string, ScanStage>();
  for (const line of lines) {
    latestStageByScanId.set(line.scanId, line.stage);
  }

  const streamCompleted =
    expectedScanIds.size > 0 &&
    [...expectedScanIds].every((scanId) => {
      const stage = latestStageByScanId.get(scanId);
      return stage !== undefined && TERMINAL_STAGES.includes(stage);
    });

  return { lines, chainByScanId, completed: initiallyCompleted || streamCompleted };
}
