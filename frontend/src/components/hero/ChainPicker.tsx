import { Card } from "@/components/ui/Card";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { Spinner } from "@/components/ui/Spinner";
import { useI18n } from "@/lib/i18n/context";
import { formatAddress } from "@/lib/format";
import type { ChainCandidate } from "@/lib/types";

interface ChainPickerProps {
  address: string;
  chains: ChainCandidate[];
  busyChainId: number | null;
  onSelect: (chainId: number) => void;
  onChangeAddress: () => void;
}

export function ChainPicker({ address, chains, busyChainId, onSelect, onChangeAddress }: ChainPickerProps) {
  const { t } = useI18n();
  const isBusy = busyChainId !== null;

  return (
    <div className="flex w-full max-w-2xl flex-col items-center gap-4">
      <Card className="w-full">
        <div className="flex flex-col gap-2">
          {chains.map((chain) => (
            <ChainRow
              key={chain.chainId}
              address={address}
              chain={chain}
              busy={busyChainId === chain.chainId}
              disabled={isBusy}
              onSelect={() => onSelect(chain.chainId)}
            />
          ))}
        </div>
      </Card>

      <button
        type="button"
        onClick={onChangeAddress}
        disabled={isBusy}
        className="font-mono text-xs uppercase tracking-widest text-text-dim transition-colors hover:text-text disabled:cursor-not-allowed disabled:text-text-faint"
      >
        {t("landing.changeAddress")}
      </button>
    </div>
  );
}

interface ChainRowProps {
  address: string;
  chain: ChainCandidate;
  busy: boolean;
  disabled: boolean;
  onSelect: () => void;
}

function ChainRow({ address, chain, busy, disabled, onSelect }: ChainRowProps) {
  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={disabled}
      aria-busy={busy}
      className="group flex w-full items-center gap-3 rounded-base border border-border bg-surface-2 px-4 py-3 text-left transition-colors hover:border-accent disabled:cursor-not-allowed disabled:hover:border-border"
    >
      <span className="flex h-6 w-6 shrink-0 items-center justify-center">
        {busy ? <Spinner /> : <ChainIcon chainId={chain.chainId} className="h-6 w-6 text-text-dim group-disabled:text-text-faint" />}
      </span>
      <span className="font-sans text-sm text-text group-disabled:text-text-faint">{chain.displayName}</span>
      <span className="ml-auto font-mono text-xs text-text-dim group-disabled:text-text-faint">
        {formatAddress(address)}
      </span>
    </button>
  );
}
