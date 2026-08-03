import { TargetChip } from "@/components/ui/TargetChip";
import { Card } from "@/components/ui/Card";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { Spinner } from "@/components/ui/Spinner";
import { useI18n } from "@/lib/i18n/context";
import { chainLabel } from "@/lib/chains";

interface ChainPickerProps {
  target: string;
  chainIds: number[];
  busyChainId: number | null;
  onSelect: (chainId: number) => void;
  onChangeTarget: () => void;
}

export function ChainPicker({ target, chainIds, busyChainId, onSelect, onChangeTarget }: ChainPickerProps) {
  const { t } = useI18n();
  const isBusy = busyChainId !== null;

  return (
    <div className="flex w-full max-w-2xl flex-col items-center gap-4">
      <Card className="w-full">
        <div className="flex flex-col gap-2">
          {chainIds.map((chainId) => (
            <ChainRow
              key={chainId}
              chainId={chainId}
              busy={busyChainId === chainId}
              disabled={isBusy}
              onSelect={() => onSelect(chainId)}
            />
          ))}
        </div>
      </Card>

      <div className="flex items-center gap-3">
        <TargetChip value={target} className="text-xs" />
        <button
          type="button"
          onClick={onChangeTarget}
          disabled={isBusy}
          className="font-mono text-xs uppercase tracking-widest text-text-dim transition-colors hover:text-text disabled:cursor-not-allowed disabled:text-text-faint"
        >
          {t("landing.changeTarget")}
        </button>
      </div>
    </div>
  );
}

interface ChainRowProps {
  chainId: number;
  busy: boolean;
  disabled: boolean;
  onSelect: () => void;
}

function ChainRow({ chainId, busy, disabled, onSelect }: ChainRowProps) {
  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={disabled}
      aria-busy={busy}
      className="group flex w-full items-center gap-3 rounded-base border border-border bg-surface-2 px-4 py-3 text-left transition-colors hover:border-accent disabled:cursor-not-allowed disabled:hover:border-border"
    >
      <span className="flex h-6 w-6 shrink-0 items-center justify-center">
        {busy ? <Spinner /> : <ChainIcon chainId={chainId} className="h-6 w-6 text-text-dim group-disabled:text-text-faint" />}
      </span>
      <span className="font-sans text-sm text-text group-disabled:text-text-faint">{chainLabel(chainId)}</span>
    </button>
  );
}
