import { useChains } from "@/lib/chains/context";
import type { Chain } from "@/lib/chains/registry";
import { formatNativeAmount } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";

interface NativeAmountProps {
  chain: Chain;
  raw: string;
}

export function NativeAmount({ chain, raw }: NativeAmountProps) {
  const { locale } = useI18n();
  const { info } = useChains();
  const chainInfo = info(chain);

  return (
    <p className="mt-2 font-mono text-2xl text-text">
      {formatNativeAmount(raw, chainInfo?.nativeDecimals ?? null, locale)}{" "}
      <span className="text-base text-text-dim">{chainInfo?.nativeSymbol ?? ""}</span>
    </p>
  );
}
