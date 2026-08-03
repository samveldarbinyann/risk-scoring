import type { ScanTarget } from "@/lib/types";

const EVM_ADDRESS_PATTERN = /^0x[a-fA-F0-9]{40}$/;
const EVM_TX_HASH_PATTERN = /^0x[a-fA-F0-9]{64}$/;

export function classifyTarget(value: string): ScanTarget | null {
  if (EVM_ADDRESS_PATTERN.test(value)) return "ADDRESS";
  if (EVM_TX_HASH_PATTERN.test(value)) return "TRANSACTION";
  return null;
}
