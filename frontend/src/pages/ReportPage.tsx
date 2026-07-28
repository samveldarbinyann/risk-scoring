import { useEffect, useState } from "react";
import { useParams } from "react-router";
import { getScanReport } from "@/lib/api";
import type { ScanReportView } from "@/lib/types";
import { VerdictReveal } from "@/components/report/VerdictReveal";
import { EvidenceList } from "@/components/report/EvidenceList";
import { GraphPlaceholder } from "@/components/report/GraphPlaceholder";
import { Spinner } from "@/components/ui/Spinner";
import { formatAddress, formatDateTime } from "@/lib/format";
import { chainLabel } from "@/lib/chains";
import { useI18n } from "@/lib/i18n/context";

export function ReportPage() {
  const { scanId } = useParams<{ scanId: string }>();
  const { t } = useI18n();
  const [report, setReport] = useState<ScanReportView | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!scanId) return;
    getScanReport(scanId)
      .then(setReport)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : t("report.loadError")));
  }, [scanId, t]);

  if (error) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-3 px-6 py-10">
        <p className="font-mono text-sm text-risk-critical">{error}</p>
      </div>
    );
  }

  if (!report) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-3 px-6 py-10">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-6 py-10">
      <header className="flex items-center justify-between gap-3">
        <div>
          <p className="font-mono text-sm text-text">{formatAddress(report.address)}</p>
          <p className="font-sans text-xs text-text-dim">
            {chainLabel(report.chainId)} · {formatDateTime(report.createdAt)}
          </p>
        </div>
        <p className="font-mono text-xs text-text-faint">{report.model}</p>
      </header>

      <VerdictReveal level={report.riskLevel} score={report.score} />
      <EvidenceList
        explanation={report.explanation}
        decisiveSignals={report.decisiveSignals}
        manualChecks={report.manualChecks}
      />
      <GraphPlaceholder />
    </div>
  );
}
