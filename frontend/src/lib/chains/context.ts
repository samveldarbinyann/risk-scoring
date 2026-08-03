import { createContext, useContext } from "react";
import type { Chain, ChainInfo } from "@/lib/chains/registry";

export interface ChainRegistryContextValue {
  chains: ChainInfo[];
  info: (chain: Chain) => ChainInfo | undefined;
  label: (chain: Chain) => string;
  symbol: (chain: Chain) => string;
  decimals: (chain: Chain) => number;
}

export const ChainRegistryContext = createContext<ChainRegistryContextValue | null>(null);

export function useChains(): ChainRegistryContextValue {
  const ctx = useContext(ChainRegistryContext);
  if (!ctx) throw new Error("useChains must be used within ChainRegistryProvider");
  return ctx;
}
