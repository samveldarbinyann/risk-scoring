export interface EvmChain {
  chainId: number;
  label: string;
  nativeSymbol: string;
}

export const EVM_CHAINS: EvmChain[] = [
  { chainId: 1, label: "Ethereum", nativeSymbol: "ETH" },
  { chainId: 10, label: "OP Mainnet", nativeSymbol: "ETH" },
  { chainId: 56, label: "BNB Smart Chain", nativeSymbol: "BNB" },
  { chainId: 100, label: "Gnosis", nativeSymbol: "xDAI" },
  { chainId: 137, label: "Polygon", nativeSymbol: "POL" },
  { chainId: 8453, label: "Base", nativeSymbol: "ETH" },
  { chainId: 42161, label: "Arbitrum One", nativeSymbol: "ETH" },
  { chainId: 43114, label: "Avalanche C-Chain", nativeSymbol: "AVAX" },
  { chainId: 59144, label: "Linea", nativeSymbol: "ETH" },
  { chainId: 11155111, label: "Sepolia", nativeSymbol: "ETH" },
];

export function chainLabel(chainId: number): string {
  return EVM_CHAINS.find((chain) => chain.chainId === chainId)?.label ?? `Chain ${chainId}`;
}

export function nativeSymbol(chainId: number): string {
  return EVM_CHAINS.find((chain) => chain.chainId === chainId)?.nativeSymbol ?? "ETH";
}
