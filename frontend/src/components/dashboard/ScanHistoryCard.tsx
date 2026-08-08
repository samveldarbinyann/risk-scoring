import { ScanHistoryRow } from "@/components/dashboard/ScanHistoryRow";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { Pagination } from "@/components/ui/Pagination";
import { Select } from "@/components/ui/Select";
import { useI18n } from "@/lib/i18n/context";
import { SCAN_SOURCE } from "@/lib/scanSource";
import type { RecentScanGroupView, ScanSource } from "@/lib/types";

interface ScanHistoryCardProps {
  scans: RecentScanGroupView[];
  page: number;
  totalPages: number;
  hasNext: boolean;
  sourceFilter: ScanSource | undefined;
  isLoading: boolean;
  error: string | null;
  onSourceChange: (source: ScanSource | undefined) => void;
  onPageChange: (page: number) => void;
}

export function ScanHistoryCard({
  scans,
  page,
  totalPages,
  hasNext,
  sourceFilter,
  isLoading,
  error,
  onSourceChange,
  onPageChange,
}: ScanHistoryCardProps) {
  const { t } = useI18n();

  const sourceOptions = [
    { value: "", label: t("dashboard.history.filterAll") },
    ...(Object.keys(SCAN_SOURCE) as ScanSource[]).map((source) => ({
      value: source,
      label: t(SCAN_SOURCE[source].labelKey),
    })),
  ];

  return (
    <Card className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <h3 className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("dashboard.history.title")}</h3>
        <Select
          options={sourceOptions}
          value={sourceFilter ?? ""}
          onChange={(value) => onSourceChange((value || undefined) as ScanSource | undefined)}
          className="w-auto"
        />
      </div>

      <CardState isLoading={isLoading} error={error}>
        {scans.length === 0 ? (
          <EmptyState message={t("dashboard.history.empty")} />
        ) : (
          <>
            <div>
              {scans.map((scan) => (
                <ScanHistoryRow key={scan.groupId} scan={scan} />
              ))}
            </div>
            <Pagination page={page} totalPages={totalPages} hasNext={hasNext} onChange={onPageChange} />
          </>
        )}
      </CardState>
    </Card>
  );
}
