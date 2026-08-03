import type { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { useChains } from "@/lib/chains/context";
import type { Chain } from "@/lib/chains/registry";
import { useI18n } from "@/lib/i18n/context";

interface WatchlistFormProps {
  address: string;
  chain: Chain;
  isSubmitting: boolean;
  onAddressChange: (value: string) => void;
  onChainChange: (value: Chain) => void;
  onSubmit: () => void;
}

export function WatchlistForm({
  address,
  chain,
  isSubmitting,
  onAddressChange,
  onChainChange,
  onSubmit,
}: WatchlistFormProps) {
  const { t } = useI18n();
  const { chains } = useChains();

  const options = chains.map((info) => ({
    value: info.chain,
    label: info.support === "PLANNED" ? `${info.displayName} — ${t("landing.chainComingSoon")}` : info.displayName,
    disabled: info.support === "PLANNED",
  }));

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
        value={chain}
        onChange={(event) => onChainChange(event.target.value as Chain)}
        options={options}
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
