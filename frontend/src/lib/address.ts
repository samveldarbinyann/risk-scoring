const EVM_ADDRESS_PATTERN = /^0x[a-fA-F0-9]{40}$/;

export function isEvmAddress(value: string): boolean {
  return EVM_ADDRESS_PATTERN.test(value);
}