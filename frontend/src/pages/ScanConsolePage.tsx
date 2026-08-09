import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { ConsoleLog } from "@/components/console/ConsoleLog";
import { ScanPipeline } from "@/components/console/ScanPipeline";
import { TargetChip } from "@/components/ui/TargetChip";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import { useScanGroupStream } from "@/hooks/useScanGroupStream";
import { useI18n } from "@/lib/i18n/context";
import type { ScanProgressMessage } from "@/lib/types";

const REPORT_TRANSITION_DELAY_MS = 900;

export function ScanConsolePage() {
  const { groupId } = useParams<{ groupId: string }>();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { lines, chainByScanId, completed, target, error } = useScanGroupStream(groupId ?? "");
  const [playedLines, setPlayedLines] = useState<ScanProgressMessage[]>([]);
  const [playbackComplete, setPlaybackComplete] = useState(false);

  useEffect(() => {
    setPlayedLines([]);
    setPlaybackComplete(false);
  }, [groupId]);

  const handlePlaybackChange = useCallback((nextLines: ScanProgressMessage[]) => setPlayedLines(nextLines), []);
  const handlePlaybackComplete = useCallback(() => setPlaybackComplete(true), []);

  useEffect(() => {
    if (error || !completed || (!playbackComplete && lines.length > 0) || !groupId) return;
    const timeout = setTimeout(() => navigate(`/scan/${groupId}/report`), REPORT_TRANSITION_DELAY_MS);
    return () => clearTimeout(timeout);
  }, [completed, error, groupId, lines.length, navigate, playbackComplete]);

  if (!groupId) return null;

  if (error) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-3 px-6 py-10">
        <ErrorMessage message={error} size="sm" />
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center px-6 py-12">
      <div className="w-full max-w-4xl">
        <div className="overflow-hidden rounded-panel border border-border bg-surface">
          <header className="flex items-center justify-between p-4">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-base border border-accent bg-surface-2">
                {!completed && <Spinner className="h-6 w-6" />}
              </div>
              <div>
                <p className="font-mono text-xs uppercase tracking-widest text-accent">{t("console.scanning")}</p>
                <p className="mt-1 font-mono text-xs text-text-faint">{t("console.pipeline")}</p>
              </div>
            </div>
            <div className="text-right">
              <div className="h-5">{target && <TargetChip value={target} className="text-sm" />}</div>
              <p className="mt-1 font-mono text-xs text-text-faint">
                {t("console.session")} {groupId.slice(0, 8).toUpperCase()}
              </p>
            </div>
          </header>
          <div className="border-t border-border">
            <ScanPipeline lines={playedLines} />
          </div>
          <div className="border-t border-border">
            <ConsoleLog
              lines={lines}
              chainByScanId={chainByScanId}
              completed={completed}
              onPlaybackChange={handlePlaybackChange}
              onPlaybackComplete={handlePlaybackComplete}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
