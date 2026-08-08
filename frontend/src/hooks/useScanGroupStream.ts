import { useEffect, useState } from "react";
import { getScanGroup } from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import { subscribeScanGroupProgress } from "@/lib/ws";
import type { Chain } from "@/lib/chains/registry";
import type { ScanProgressMessage, ScanStage, ScanTarget } from "@/lib/types";

const TERMINAL_STAGES: ScanStage[] = ["COMPLETED", "FAILED"];

interface Failure {
  reason: string | null;
}

interface ScanGroupStreamState {
  lines: ScanProgressMessage[];
  chainByScanId: Map<string, Chain>;
  completed: boolean;
  targetType: ScanTarget | null;
  target: string;
  error: string | null;
}

export function useScanGroupStream(groupId: string): ScanGroupStreamState {
  const { status } = useAuth();
  const { t } = useI18n();
  const [lines, setLines] = useState<ScanProgressMessage[]>([]);
  const [chainByScanId, setChainByScanId] = useState<Map<string, Chain>>(new Map());
  const [expectedScanIds, setExpectedScanIds] = useState<Set<string>>(new Set());
  const [initiallyCompleted, setInitiallyCompleted] = useState(false);
  const [targetType, setTargetType] = useState<ScanTarget | null>(null);
  const [target, setTarget] = useState("");
  const [failure, setFailure] = useState<Failure | null>(null);

  useEffect(() => {
    setLines([]);
    setChainByScanId(new Map());
    setExpectedScanIds(new Set());
    setInitiallyCompleted(false);
    setTargetType(null);
    setTarget("");
    setFailure(null);
    if (!groupId || status === "loading") return;

    let cancelled = false;
    getScanGroup(groupId)
      .then((group) => {
        if (cancelled) return;
        setChainByScanId(new Map(group.chains.map((chain) => [chain.scanId, chain.chain])));
        setExpectedScanIds(new Set(group.chains.map((chain) => chain.scanId)));
        setInitiallyCompleted(group.completed);
        setTargetType(group.targetType);
        setTarget(group.target);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setFailure({ reason: err instanceof Error ? err.message : null });
      });

    const unsubscribe = subscribeScanGroupProgress(
      groupId,
      (message) => setLines((prev) => [...prev, message]),
      (reason) => {
        if (!cancelled) setFailure({ reason: reason ?? null });
      },
    );

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [groupId, status]);

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

  return {
    lines,
    chainByScanId,
    completed: initiallyCompleted || streamCompleted,
    targetType,
    target,
    error: failure && (failure.reason ?? t("console.loadError")),
  };
}
