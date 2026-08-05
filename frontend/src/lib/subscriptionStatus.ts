import type { MessageKey } from "@/lib/i18n/messageKeys";
import type { SubscriptionStatus } from "@/lib/types";

export const SUBSCRIPTION_STATUS_CLASS: Record<SubscriptionStatus, string> = {
  ACTIVE: "border-accent text-accent",
  PENDING_PAYMENT: "border-risk-mid text-risk-mid",
  CANCELED: "border-border text-text-faint",
  EXPIRED: "border-border text-text-faint",
};

export const SUBSCRIPTION_STATUS_KEY: Record<SubscriptionStatus, MessageKey> = {
  ACTIVE: "settings.status.ACTIVE",
  PENDING_PAYMENT: "settings.status.PENDING_PAYMENT",
  CANCELED: "settings.status.CANCELED",
  EXPIRED: "settings.status.EXPIRED",
};
