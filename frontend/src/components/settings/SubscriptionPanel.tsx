import { Link } from "react-router";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { QuotaBar } from "@/components/settings/QuotaBar";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/lib/cn";
import { formatDateTime, formatMoney } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import type { SubscriptionStatus, SubscriptionView } from "@/lib/types";

const STATUS_CLASS: Record<SubscriptionStatus, string> = {
  ACTIVE: "border-accent text-accent",
  PENDING_PAYMENT: "border-risk-mid text-risk-mid",
  CANCELED: "border-border text-text-faint",
  EXPIRED: "border-border text-text-faint",
};

const STATUS_KEY: Record<SubscriptionStatus, MessageKey> = {
  ACTIVE: "settings.status.ACTIVE",
  PENDING_PAYMENT: "settings.status.PENDING_PAYMENT",
  CANCELED: "settings.status.CANCELED",
  EXPIRED: "settings.status.EXPIRED",
};

interface SubscriptionPanelProps {
  subscription: SubscriptionView | null;
  isLoading: boolean;
  error: string | null;
  actionError: string | null;
  isConfirming: boolean;
  isCanceling: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function SubscriptionPanel({
  subscription,
  isLoading,
  error,
  actionError,
  isConfirming,
  isCanceling,
  onConfirm,
  onCancel,
}: SubscriptionPanelProps) {
  const { t, locale } = useI18n();

  return (
    <Card title={t("settings.subscription.title")}>
      {isLoading ? (
        <div className="flex justify-center py-8">
          <Spinner />
        </div>
      ) : error ? (
        <ErrorMessage message={error} size="sm" />
      ) : !subscription ? (
        <div className="flex flex-col gap-4">
          <p className="font-mono text-sm text-text-faint">{t("settings.subscription.empty")}</p>
          <Link
            to="/pricing"
            className="inline-flex h-12 w-fit items-center justify-center rounded-base bg-accent px-6 font-sans text-base font-medium text-bg transition-colors hover:bg-accent-press"
          >
            {t("settings.subscription.ctaPricing")}
          </Link>
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <MetaRow label={t("settings.subscription.plan")} value={subscription.planCode} mono />
            <div className="space-y-1">
              <p className="font-sans text-xs uppercase tracking-wider text-text-faint">
                {t("settings.subscription.status")}
              </p>
              <span
                className={cn(
                  "inline-flex items-center rounded-base border px-2.5 py-1 font-mono text-xs uppercase tracking-wider",
                  STATUS_CLASS[subscription.status],
                )}
              >
                {t(STATUS_KEY[subscription.status])}
              </span>
            </div>
            <MetaRow
              label={t("settings.subscription.price")}
              value={`${formatMoney(subscription.priceCents, subscription.currency, locale)} ${t("pricing.perMonth")}`}
              mono
            />
            <MetaRow
              label={t("settings.subscription.period")}
              value={
                subscription.currentPeriodStart && subscription.currentPeriodEnd
                  ? `${formatDateTime(subscription.currentPeriodStart)} → ${formatDateTime(subscription.currentPeriodEnd)}`
                  : "—"
              }
              mono
            />
          </div>

          {subscription.status === "ACTIVE" && (
            <QuotaBar
              used={subscription.requestsUsed}
              limit={subscription.monthlyRequestLimit}
              remaining={subscription.requestsRemaining}
            />
          )}

          <div className="flex flex-wrap gap-3">
            {subscription.status === "PENDING_PAYMENT" && (
              <Button type="button" isLoading={isConfirming} onClick={onConfirm}>
                {t("settings.subscription.confirm")}
              </Button>
            )}
            {(subscription.status === "ACTIVE" || subscription.status === "PENDING_PAYMENT") && (
              <Button type="button" variant="ghost" isLoading={isCanceling} onClick={onCancel}>
                {t("settings.subscription.cancel")}
              </Button>
            )}
            {(subscription.status === "CANCELED" || subscription.status === "EXPIRED") && (
              <Link
                to="/pricing"
                className="inline-flex h-12 items-center justify-center rounded-base bg-accent px-6 font-sans text-base font-medium text-bg transition-colors hover:bg-accent-press"
              >
                {t("settings.subscription.ctaPricing")}
              </Link>
            )}
          </div>

          <ErrorMessage message={actionError} size="sm" />
        </div>
      )}
    </Card>
  );
}

function MetaRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="space-y-1">
      <p className="font-sans text-xs uppercase tracking-wider text-text-faint">{label}</p>
      <p className={cn("text-sm text-text", mono && "font-mono")}>{value}</p>
    </div>
  );
}
