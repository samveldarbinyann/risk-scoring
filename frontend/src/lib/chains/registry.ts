export type Chain =
  | "ETHEREUM"
  | "OPTIMISM"
  | "BNB_SMART_CHAIN"
  | "GNOSIS"
  | "POLYGON"
  | "BASE"
  | "ARBITRUM_ONE"
  | "AVALANCHE"
  | "LINEA"
  | "SEPOLIA"
  | "BITCOIN"
  | "SOLANA"
  | "TRON"
  | "TON"
  | "SUI";

export type ChainFamily = "EVM" | "BITCOIN" | "SOLANA" | "TRON" | "TON" | "SUI";

export type ChainSupport = "SUPPORTED" | "PLANNED";

export interface ChainInfo {
  chain: Chain;
  family: ChainFamily;
  displayName: string;
  nativeSymbol: string;
  nativeDecimals: number;
  evmChainId: number | null;
  mainnet: boolean;
  support: ChainSupport;
}

export const DEFAULT_NATIVE_DECIMALS = 18;
