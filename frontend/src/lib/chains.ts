export interface EvmChain {
  chainId: number;
  label: string;
}

export const EVM_CHAINS: EvmChain[] = [
  { chainId: 1, label: "Ethereum" },
  { chainId: 137, label: "Polygon" },
  { chainId: 42161, label: "Arbitrum One" },
  { chainId: 59144, label: "Linea" },
  { chainId: 100, label: "Gnosis" },
  { chainId: 11155111, label: "Sepolia" },
];

export function chainLabel(chainId: number): string {
  return EVM_CHAINS.find((chain) => chain.chainId === chainId)?.label ?? `Chain ${chainId}`;
}
