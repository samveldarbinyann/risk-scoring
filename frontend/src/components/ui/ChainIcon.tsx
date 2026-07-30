import type { ComponentType } from "react";
import type { IconComponentProps } from "@web3icons/react";
import NetworkArbitrumOne from "@web3icons/react/icons/networks/NetworkArbitrumOne";
import NetworkAvalanche from "@web3icons/react/icons/networks/NetworkAvalanche";
import NetworkBase from "@web3icons/react/icons/networks/NetworkBase";
import NetworkBinanceSmartChain from "@web3icons/react/icons/networks/NetworkBinanceSmartChain";
import NetworkEthereum from "@web3icons/react/icons/networks/NetworkEthereum";
import NetworkGnosis from "@web3icons/react/icons/networks/NetworkGnosis";
import NetworkLinea from "@web3icons/react/icons/networks/NetworkLinea";
import NetworkOptimism from "@web3icons/react/icons/networks/NetworkOptimism";
import NetworkPolygon from "@web3icons/react/icons/networks/NetworkPolygon";
import { cn } from "@/lib/cn";

const ICON_BY_CHAIN_ID: Record<number, ComponentType<IconComponentProps>> = {
  1: NetworkEthereum,
  10: NetworkOptimism,
  56: NetworkBinanceSmartChain,
  100: NetworkGnosis,
  137: NetworkPolygon,
  8453: NetworkBase,
  42161: NetworkArbitrumOne,
  43114: NetworkAvalanche,
  59144: NetworkLinea,
};

interface ChainIconProps {
  chainId: number;
  className?: string;
}

export function ChainIcon({ chainId, className }: ChainIconProps) {
  const Icon = ICON_BY_CHAIN_ID[chainId];
  if (!Icon) return null;

  return <Icon variant="mono" className={cn("[&_path]:fill-current", className)} />;
}
