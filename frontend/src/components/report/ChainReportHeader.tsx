import { ChainIcon } from "@/components/ui/ChainIcon";
import { useChains } from "@/lib/chains/context";
import type { Chain } from "@/lib/chains/registry";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";

interface ChainReportHeaderProps {
  chain: Chain;
  createdAt: string;
}

export function ChainReportHeader({ chain, createdAt }: ChainReportHeaderProps) {
  const { locale } = useI18n();
  const { label } = useChains();

  return (
    <header className="flex items-center justify-between gap-3 p-6">
      <div className="flex items-center gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-base border border-accent bg-surface-2">
          <ChainIcon chain={chain} className="h-6 w-6 text-accent" />
        </div>
        <p className="font-mono text-sm uppercase tracking-widest text-text">{label(chain)}</p>
      </div>
      <p className="font-mono text-xs text-text-faint">{formatDateTime(createdAt, locale)}</p>
    </header>
  );
}
