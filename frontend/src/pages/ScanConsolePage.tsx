import { useEffect } from "react";
import { useNavigate, useParams } from "react-router";
import { ConsoleLog } from "@/components/console/ConsoleLog";
import { Spinner } from "@/components/ui/Spinner";
import { useScanGroupStream } from "@/hooks/useScanGroupStream";
import { useI18n } from "@/lib/i18n/context";

const REPORT_TRANSITION_DELAY_MS = 900;

export function ScanConsolePage() {
  const { groupId } = useParams<{ groupId: string }>();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { lines, chainByScanId, completed } = useScanGroupStream(groupId ?? "");

  useEffect(() => {
    if (!completed || !groupId) return;
    const timeout = setTimeout(() => navigate(`/scan/${groupId}/report`), REPORT_TRANSITION_DELAY_MS);
    return () => clearTimeout(timeout);
  }, [completed, groupId, navigate]);

  if (!groupId) return null;

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 px-6">
      <header className="flex items-center gap-3">
        {!completed && <Spinner />}
        <div>
          <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("console.title")}</p>
          <p className="font-mono text-xs text-text-faint">{groupId}</p>
        </div>
      </header>

      <ConsoleLog lines={lines} chainByScanId={chainByScanId} connectingText={t("console.connecting")} />
    </div>
  );
}
