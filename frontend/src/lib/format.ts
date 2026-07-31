const WEI_PER_ETHER = 1_000_000_000_000_000_000n;

export function formatWei(wei: string, maxFractionDigits = 4): string {
  const value = BigInt(wei);
  const whole = value / WEI_PER_ETHER;
  const fraction = value % WEI_PER_ETHER;

  const fractionStr = fraction.toString().padStart(18, "0").slice(0, maxFractionDigits);
  const trimmedFraction = fractionStr.replace(/0+$/, "");

  return trimmedFraction ? `${whole}.${trimmedFraction}` : whole.toString();
}

export function truncateId(value: string): string {
  if (value.length <= 12) return value;
  return `${value.slice(0, 6)}…${value.slice(-4)}`;
}

export function formatAddress(address: string): string {
  return truncateId(address);
}

export function formatDateTime(iso: string, locale?: string): string {
  return new Date(iso).toLocaleString(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export function formatTime(iso: string, locale?: string): string {
  return new Date(iso).toLocaleTimeString(locale, { timeStyle: "medium" });
}

export function formatMoney(cents: number, currency: string, locale?: string): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(cents / 100);
}

export function formatCount(value: number, locale?: string): string {
  return new Intl.NumberFormat(locale).format(value);
}
