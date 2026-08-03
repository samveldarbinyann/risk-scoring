import { ChainIcon } from "@/components/ui/ChainIcon";
import { chainLabel } from "@/lib/chains";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";

interface ChainReportHeaderProps {
  chainId: number;
  createdAt: string;
}

export function ChainReportHeader({ chainId, createdAt }: ChainReportHeaderProps) {
  const { locale } = useI18n();

  return (
    <header className="flex items-center justify-between gap-3 p-6">
      <div className="flex items-center gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-base border border-accent bg-surface-2">
          <ChainIcon chainId={chainId} className="h-6 w-6 text-accent" />
        </div>
        <p className="font-mono text-sm uppercase tracking-widest text-text">{chainLabel(chainId)}</p>
      </div>
      <p className="font-mono text-xs text-text-faint">{formatDateTime(createdAt, locale)}</p>
    </header>
  );
}
