import { useEffect } from "react";
import { useNavigate, useParams } from "react-router";
import { ConsoleLog } from "@/components/console/ConsoleLog";
import { Spinner } from "@/components/ui/Spinner";
import { useScanStream } from "@/hooks/useScanStream";
import { useI18n } from "@/lib/i18n/context";

const REPORT_TRANSITION_DELAY_MS = 900;

export function ScanConsolePage() {
  const { scanId } = useParams<{ scanId: string }>();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { lines, latestStage } = useScanStream(scanId ?? "");

  useEffect(() => {
    if (latestStage !== "COMPLETED" || !scanId) return;
    const timeout = setTimeout(() => navigate(`/scan/${scanId}/report`), REPORT_TRANSITION_DELAY_MS);
    return () => clearTimeout(timeout);
  }, [latestStage, scanId, navigate]);

  if (!scanId) return null;

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-6 py-10">
      <header className="flex items-center gap-3">
        {latestStage !== "COMPLETED" && latestStage !== "FAILED" && <Spinner />}
        <div>
          <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("console.title")}</p>
          <p className="font-mono text-xs text-text-faint">{scanId}</p>
        </div>
      </header>

      <div className="min-h-0 flex-1 overflow-y-auto rounded-base border border-border bg-surface p-6">
        {lines.length === 0 ? (
          <p className="font-mono text-sm text-text-faint">{t("console.connecting")}</p>
        ) : (
          <ConsoleLog lines={lines} />
        )}
      </div>
    </div>
  );
}
