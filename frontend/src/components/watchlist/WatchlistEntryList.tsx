import { WatchlistEntryRow } from "@/components/watchlist/WatchlistEntryRow";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { Pagination } from "@/components/ui/Pagination";
import { formatCount } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { WatchlistEntryView } from "@/lib/types";

const PAGE_SIZE = 8;

interface WatchlistEntryListProps {
  entries: WatchlistEntryView[];
  page: number;
  isLoading: boolean;
  error: string | null;
  removingId: string | null;
  onPageChange: (page: number) => void;
  onRemove: (id: string) => void;
}

export function WatchlistEntryList({
  entries,
  page,
  isLoading,
  error,
  removingId,
  onPageChange,
  onRemove,
}: WatchlistEntryListProps) {
  const { t, locale } = useI18n();

  const totalPages = Math.ceil(entries.length / PAGE_SIZE);
  const pageEntries = entries.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const hasNext = (page + 1) * PAGE_SIZE < entries.length;

  return (
    <Card className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <h3 className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("watchlist.title")}</h3>
        {entries.length > 0 && (
          <span className="font-mono text-xs text-text-faint">{formatCount(entries.length, locale)}</span>
        )}
      </div>

      <CardState isLoading={isLoading} error={error}>
        {entries.length === 0 ? (
          <EmptyState message={t("watchlist.empty")} hint={t("watchlist.empty.hint")} />
        ) : (
          <>
            <div>
              {pageEntries.map((entry) => (
                <WatchlistEntryRow
                  key={entry.id}
                  entry={entry}
                  isRemoving={removingId === entry.id}
                  onRemove={onRemove}
                />
              ))}
            </div>
            <Pagination page={page} totalPages={totalPages} hasNext={hasNext} onChange={onPageChange} />
          </>
        )}
      </CardState>
    </Card>
  );
}
