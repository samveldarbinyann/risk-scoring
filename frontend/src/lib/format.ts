export function formatWei(wei: string, locale?: string, maxFractionDigits = 4): string {
  const value = BigInt(wei);
  const scale = 10n ** BigInt(18 - maxFractionDigits);
  const unit = 10n ** BigInt(maxFractionDigits);
  const roundedScaled = (value + scale / 2n) / scale;

  const whole = roundedScaled / unit;
  const fraction = roundedScaled % unit;
  const fractionStr = fraction.toString().padStart(maxFractionDigits, "0").replace(/0+$/, "");
  const groupedWhole = new Intl.NumberFormat(locale).format(whole);

  return fractionStr ? `${groupedWhole}.${fractionStr}` : groupedWhole;
}

export function formatTokenAmount(balanceFormatted: string, locale?: string, maxFractionDigits = 4): string {
  const value = Number(balanceFormatted);
  if (!Number.isFinite(value)) return balanceFormatted;

  return new Intl.NumberFormat(locale, { maximumFractionDigits: maxFractionDigits }).format(value);
}

export function formatUsd(value: number, locale?: string): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: value < 1 ? 4 : 2,
  }).format(value);
}

const DISPLAYABLE_SYMBOL = /^[A-Za-z0-9.$+\-_ ]{1,15}$/;

export function isDisplayableSymbol(symbol: string): boolean {
  return DISPLAYABLE_SYMBOL.test(symbol);
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
