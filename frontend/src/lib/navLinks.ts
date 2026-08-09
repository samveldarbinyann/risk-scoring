import type { MessageKey } from "@/lib/i18n/messageKeys";

export interface NavLinkItem {
  to: string;
  labelKey: MessageKey;
}

export const ACCOUNT_LINKS: readonly NavLinkItem[] = [
  { to: "/dashboard", labelKey: "nav.dashboard" },
  { to: "/watchlist", labelKey: "nav.watchlist" },
  { to: "/alerts", labelKey: "nav.alerts" },
  { to: "/settings", labelKey: "nav.settings" },
];
