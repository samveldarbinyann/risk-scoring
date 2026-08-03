import { useEffect, useMemo, useState, type ReactNode } from "react";
import { getChainRegistry } from "@/lib/api";
import { ChainRegistryContext, type ChainRegistryContextValue } from "@/lib/chains/context";
import type { Chain, ChainInfo } from "@/lib/chains/registry";

export function ChainRegistryProvider({ children }: { children: ReactNode }) {
  const [chains, setChains] = useState<ChainInfo[]>([]);

  useEffect(() => {
    const abort = new AbortController();

    getChainRegistry({ signal: abort.signal })
      .then((registry) => {
        if (!abort.signal.aborted) setChains(registry);
      })
      .catch((error) => {
        if (!abort.signal.aborted) console.error("Chain registry unavailable", error);
      });

    return () => {
      abort.abort();
    };
  }, []);

  const value = useMemo<ChainRegistryContextValue>(() => {
    const byChain = new Map(chains.map((entry) => [entry.chain, entry]));
    const info = (chain: Chain) => byChain.get(chain);

    return {
      chains,
      ready: chains.length > 0,
      defaultChain: chains.find((entry) => entry.support === "SUPPORTED")?.chain ?? null,
      info,
      label: (chain) => info(chain)?.displayName ?? chain,
    };
  }, [chains]);

  return <ChainRegistryContext value={value}>{children}</ChainRegistryContext>;
}
