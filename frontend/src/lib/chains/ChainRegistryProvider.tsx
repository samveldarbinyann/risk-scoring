import { useEffect, useMemo, useState, type ReactNode } from "react";
import { getChainRegistry } from "@/lib/api";
import { ChainRegistryContext, type ChainRegistryContextValue } from "@/lib/chains/context";
import { DEFAULT_NATIVE_DECIMALS, type Chain, type ChainInfo } from "@/lib/chains/registry";

export function ChainRegistryProvider({ children }: { children: ReactNode }) {
  const [chains, setChains] = useState<ChainInfo[]>([]);

  useEffect(() => {
    const abort = new AbortController();

    getChainRegistry({ signal: abort.signal })
      .then((registry) => {
        if (!abort.signal.aborted) setChains(registry);
      })
      .catch(() => undefined);

    return () => {
      abort.abort();
    };
  }, []);

  const value = useMemo<ChainRegistryContextValue>(() => {
    const info = (chain: Chain) => chains.find((entry) => entry.chain === chain);

    return {
      chains,
      info,
      label: (chain) => info(chain)?.displayName ?? chain,
      symbol: (chain) => info(chain)?.nativeSymbol ?? "",
      decimals: (chain) => info(chain)?.nativeDecimals ?? DEFAULT_NATIVE_DECIMALS,
    };
  }, [chains]);

  return <ChainRegistryContext value={value}>{children}</ChainRegistryContext>;
}
