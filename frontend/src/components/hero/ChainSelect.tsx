import { Select } from "@/components/ui/Select";
import { EVM_CHAINS } from "@/lib/chains";
import { useI18n } from "@/lib/i18n/context";

interface ChainSelectProps {
  value: number;
  onChange: (chainId: number) => void;
  disabled?: boolean;
}

const CHAIN_OPTIONS = EVM_CHAINS.map((chain) => ({ value: String(chain.chainId), label: chain.label }));

export function ChainSelect({ value, onChange, disabled }: ChainSelectProps) {
  const { t } = useI18n();

  return (
    <Select
      value={String(value)}
      onChange={(event) => onChange(Number(event.target.value))}
      options={CHAIN_OPTIONS}
      disabled={disabled}
      className="font-sans"
      aria-label={t("landing.networkLabel")}
    />
  );
}
