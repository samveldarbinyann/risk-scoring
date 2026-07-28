import { useEffect, useState } from "react";
import { useParams } from "react-router";
import { getScanGroupReport } from "@/lib/api";
import type { ScanGroupReportView } from "@/lib/types";
import { VerdictReveal } from "@/components/report/VerdictReveal";
import { EvidenceList } from "@/components/report/EvidenceList";
import { GraphPlaceholder } from "@/components/report/GraphPlaceholder";
import { Spinner } from "@/components/ui/Spinner";
import { formatAddress, formatDateTime } from "@/lib/format";
import { chainLabel } from "@/lib/chains";
import { useI18n } from "@/lib/i18n/context";

export function ReportPage() {
  const { groupId } = useParams<{ groupId: string }>();
  const { t } = useI18n();
  const [group, setGroup] = useState<ScanGroupReportView | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!groupId) return;
    setError(null);
    getScanGroupReport(groupId)
      .then((data) => {
        setGroup(data);
        setError(null);
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : t("report.loadError")));
  }, [groupId, t]);

  if (!group && error) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-3 px-6 py-10">
        <p className="font-mono text-sm text-risk-critical">{error}</p>
      </div>
    );
  }

  if (!group) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-3 px-6 py-10">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-10 px-6 py-10">
      <header>
        <p className="font-mono text-sm text-text">{formatAddress(group.address)}</p>
      </header>

      {group.reports.map((report) => (
        <div key={report.chainId} className="flex flex-col gap-6 border-t border-border pt-8 first:border-t-0 first:pt-0">
          <div className="flex items-center justify-between gap-3">
            <p className="font-mono text-sm uppercase tracking-widest text-accent">{chainLabel(report.chainId)}</p>
            <p className="font-mono text-xs text-text-faint">
              {report.model} · {formatDateTime(report.createdAt)}
            </p>
          </div>
          <VerdictReveal level={report.riskLevel} score={report.score} />
          <EvidenceList
            explanation={report.explanation}
            decisiveSignals={report.decisiveSignals}
            manualChecks={report.manualChecks}
          />
        </div>
      ))}

      <GraphPlaceholder />
    </div>
  );
}
