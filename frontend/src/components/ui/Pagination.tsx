import { Button } from "@/components/ui/Button";
import { useI18n } from "@/lib/i18n/context";

interface PaginationProps {
  page: number;
  totalPages: number;
  hasNext: boolean;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, hasNext, onChange }: PaginationProps) {
  const { t } = useI18n();

  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between gap-4 pt-2">
      <Button variant="ghost" disabled={page <= 0} onClick={() => onChange(page - 1)} className="h-9 px-4 text-sm">
        {t("pagination.prev")}
      </Button>
      <span className="font-mono text-xs text-text-faint">
        {page + 1} / {totalPages}
      </span>
      <Button variant="ghost" disabled={!hasNext} onClick={() => onChange(page + 1)} className="h-9 px-4 text-sm">
        {t("pagination.next")}
      </Button>
    </div>
  );
}
