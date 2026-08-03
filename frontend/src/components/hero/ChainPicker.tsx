import { TargetChip } from "@/components/ui/TargetChip";
import { Card } from "@/components/ui/Card";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { Spinner } from "@/components/ui/Spinner";
import { useI18n } from "@/lib/i18n/context";
import type { Chain } from "@/lib/chains/registry";
import type { ChainCandidate } from "@/lib/types";

interface ChainPickerProps {
  target: string;
  candidates: ChainCandidate[];
  busyChain: Chain | null;
  onSelect: (chain: Chain) => void;
  onChangeTarget: () => void;
}

export function ChainPicker({ target, candidates, busyChain, onSelect, onChangeTarget }: ChainPickerProps) {
  const { t } = useI18n();
  const isBusy = busyChain !== null;
  const mixedTargetTypes = new Set(candidates.map((candidate) => candidate.targetType)).size > 1;

  return (
    <div className="flex w-full max-w-2xl flex-col items-center gap-4">
      <Card className="w-full">
        <div className="flex flex-col gap-2">
          {candidates.map((candidate) => (
            <ChainRow
              key={candidate.chain}
              candidate={candidate}
              showTargetType={mixedTargetTypes}
              busy={busyChain === candidate.chain}
              disabled={isBusy || candidate.support === "PLANNED"}
              onSelect={() => onSelect(candidate.chain)}
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
  candidate: ChainCandidate;
  showTargetType: boolean;
  busy: boolean;
  disabled: boolean;
  onSelect: () => void;
}

function ChainRow({ candidate, showTargetType, busy, disabled, onSelect }: ChainRowProps) {
  const { t } = useI18n();
  const planned = candidate.support === "PLANNED";

  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={disabled}
      aria-busy={busy}
      className="group flex w-full items-center gap-3 rounded-base border border-border bg-surface-2 px-4 py-3 text-left transition-colors hover:border-accent disabled:cursor-not-allowed disabled:hover:border-border"
    >
      <span className="flex h-6 w-6 shrink-0 items-center justify-center">
        {busy ? (
          <Spinner />
        ) : (
          <ChainIcon chain={candidate.chain} className="h-6 w-6 text-text-dim group-disabled:text-text-faint" />
        )}
      </span>
      <span className="font-sans text-sm text-text group-disabled:text-text-faint">{candidate.displayName}</span>

      {showTargetType && (
        <span className="font-mono text-xs uppercase tracking-widest text-text-faint">
          {candidate.targetType === "TRANSACTION" ? t("landing.targetTypeTransaction") : t("landing.targetTypeAddress")}
        </span>
      )}

      {planned && (
        <span className="ml-auto font-mono text-xs uppercase tracking-widest text-text-faint">
          {t("landing.chainComingSoon")}
        </span>
      )}
    </button>
  );
}
