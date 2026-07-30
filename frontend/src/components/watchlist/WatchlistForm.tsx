import type { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { EVM_CHAINS } from "@/lib/chains";
import { useI18n } from "@/lib/i18n/context";

interface WatchlistFormProps {
  address: string;
  chainId: number;
  isSubmitting: boolean;
  onAddressChange: (value: string) => void;
  onChainIdChange: (value: number) => void;
  onSubmit: () => void;
}

const CHAIN_OPTIONS = EVM_CHAINS.map((chain) => ({
  value: String(chain.chainId),
  label: chain.label,
}));

export function WatchlistForm({
  address,
  chainId,
  isSubmitting,
  onAddressChange,
  onChainIdChange,
  onSubmit,
}: WatchlistFormProps) {
  const { t } = useI18n();

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form className="flex flex-col gap-3 sm:flex-row sm:items-center" onSubmit={handleSubmit}>
      <Input
        value={address}
        onChange={(event) => onAddressChange(event.target.value)}
        placeholder={t("watchlist.addressPlaceholder")}
        autoCapitalize="none"
        autoCorrect="off"
        spellCheck={false}
        disabled={isSubmitting}
        className="sm:flex-1"
      />
      <Select
        value={String(chainId)}
        onChange={(event) => onChainIdChange(Number(event.target.value))}
        options={CHAIN_OPTIONS}
        disabled={isSubmitting}
        aria-label={t("watchlist.chain")}
        className="sm:w-56"
      />
      <Button type="submit" isLoading={isSubmitting} className="sm:w-auto">
        {t("watchlist.add")}
      </Button>
    </form>
  );
}
