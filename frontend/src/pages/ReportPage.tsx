import { useEffect, useState } from "react";
import { useParams } from "react-router";
import { getScanGroupReport } from "@/lib/api";
import type { ScanGroupReportView } from "@/lib/types";
import { ChainReportHeader } from "@/components/report/ChainReportHeader";
import { VerdictReveal } from "@/components/report/VerdictReveal";
import { WalletStats } from "@/components/report/WalletStats";
import { TransactionDetails } from "@/components/report/TransactionDetails";
import { EvidenceList } from "@/components/report/EvidenceList";
import { GraphPlaceholder } from "@/components/report/GraphPlaceholder";
import { Spinner } from "@/components/ui/Spinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { TargetChip } from "@/components/ui/TargetChip";
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
        <ErrorMessage message={error} size="sm" />
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
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 px-6 py-10">
      <header>
        <TargetChip value={group.target} className="text-sm" />
      </header>

      {group.reports.map((report) => (
        <div key={report.chain} className="overflow-hidden rounded-panel border border-border bg-surface">
          <ChainReportHeader chain={report.chain} createdAt={report.createdAt} />
          <div className="border-t border-border">
            <VerdictReveal level={report.riskLevel} score={report.score} />
          </div>
          <div className="border-t border-border">
            {report.evidence.targetType === "ADDRESS" ? (
              <WalletStats
                chain={report.chain}
                balanceNative={report.evidence.balanceNative}
                tokenBalances={report.evidence.tokenBalances}
                txCount={report.evidence.txCount}
                txCount24h={report.evidence.txCount24h}
                sampleTruncated={report.evidence.sampleTruncated}
                observedAt={report.evidence.observedAt}
              />
            ) : (
              <TransactionDetails chain={report.chain} evidence={report.evidence} />
            )}
          </div>
          <div className="border-t border-border">
            <EvidenceList
              explanation={report.explanation}
              decisiveSignals={report.decisiveSignals}
              manualChecks={report.manualChecks}
            />
          </div>
        </div>
      ))}

      <GraphPlaceholder />
    </div>
  );
}
