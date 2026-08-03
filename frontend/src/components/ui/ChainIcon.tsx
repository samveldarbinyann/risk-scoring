import type { ComponentType } from "react";
import type { IconComponentProps } from "@web3icons/react";
import NetworkArbitrumOne from "@web3icons/react/icons/networks/NetworkArbitrumOne";
import NetworkAvalanche from "@web3icons/react/icons/networks/NetworkAvalanche";
import NetworkBase from "@web3icons/react/icons/networks/NetworkBase";
import NetworkBinanceSmartChain from "@web3icons/react/icons/networks/NetworkBinanceSmartChain";
import NetworkBitcoin from "@web3icons/react/icons/networks/NetworkBitcoin";
import NetworkEthereum from "@web3icons/react/icons/networks/NetworkEthereum";
import NetworkGnosis from "@web3icons/react/icons/networks/NetworkGnosis";
import NetworkLinea from "@web3icons/react/icons/networks/NetworkLinea";
import NetworkOptimism from "@web3icons/react/icons/networks/NetworkOptimism";
import NetworkPolygon from "@web3icons/react/icons/networks/NetworkPolygon";
import NetworkSolana from "@web3icons/react/icons/networks/NetworkSolana";
import NetworkSui from "@web3icons/react/icons/networks/NetworkSui";
import NetworkTon from "@web3icons/react/icons/networks/NetworkTon";
import NetworkTron from "@web3icons/react/icons/networks/NetworkTron";
import type { Chain } from "@/lib/chains/registry";
import { cn } from "@/lib/cn";

const ICON_BY_CHAIN: Partial<Record<Chain, ComponentType<IconComponentProps>>> = {
  ETHEREUM: NetworkEthereum,
  OPTIMISM: NetworkOptimism,
  BNB_SMART_CHAIN: NetworkBinanceSmartChain,
  GNOSIS: NetworkGnosis,
  POLYGON: NetworkPolygon,
  BASE: NetworkBase,
  ARBITRUM_ONE: NetworkArbitrumOne,
  AVALANCHE: NetworkAvalanche,
  LINEA: NetworkLinea,
  BITCOIN: NetworkBitcoin,
  SOLANA: NetworkSolana,
  TRON: NetworkTron,
  TON: NetworkTon,
  SUI: NetworkSui,
};

interface ChainIconProps {
  chain: Chain;
  className?: string;
}

export function ChainIcon({ chain, className }: ChainIconProps) {
  const Icon = ICON_BY_CHAIN[chain];
  if (!Icon) return null;

  return <Icon variant="mono" className={cn("[&_path]:fill-current", className)} />;
}
