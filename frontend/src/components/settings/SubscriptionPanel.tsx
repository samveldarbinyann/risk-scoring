import { useNavigate } from "react-router";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { QuotaBar } from "@/components/settings/QuotaBar";
import { cn } from "@/lib/cn";
import { formatDateTime, formatMoney } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { SUBSCRIPTION_STATUS_CLASS, SUBSCRIPTION_STATUS_KEY } from "@/lib/subscriptionStatus";
import type { SubscriptionView } from "@/lib/types";

interface SubscriptionPanelProps {
  subscription: SubscriptionView | null;
  isLoading: boolean;
  error: string | null;
  actionError: string | null;
  isCanceling: boolean;
  onCancel: () => void;
}

export function SubscriptionPanel({
  subscription,
  isLoading,
  error,
  actionError,
  isCanceling,
  onCancel,
}: SubscriptionPanelProps) {
  const { t, locale } = useI18n();
  const navigate = useNavigate();

  return (
    <Card title={t("settings.subscription.title")}>
      <CardState isLoading={isLoading} error={error}>
        {!subscription ? (
          <EmptyState
            message={t("settings.subscription.empty")}
            ctaLabel={t("settings.subscription.ctaPricing")}
            ctaTo="/pricing"
          />
        ) : (
          <div className="flex flex-col gap-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <MetaRow
                label={t("settings.subscription.plan")}
                value={t(`pricing.plan.${subscription.planCode}` as MessageKey)}
              />
              <div className="space-y-1">
                <p className="font-sans text-xs uppercase tracking-wider text-text-faint">
                  {t("settings.subscription.status")}
                </p>
                <span
                  className={cn(
                    "inline-flex items-center rounded-base border px-2.5 py-1 font-mono text-xs uppercase tracking-wider",
                    SUBSCRIPTION_STATUS_CLASS[subscription.status],
                  )}
                >
                  {t(SUBSCRIPTION_STATUS_KEY[subscription.status])}
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
                    ? `${formatDateTime(subscription.currentPeriodStart, locale)} → ${formatDateTime(subscription.currentPeriodEnd, locale)}`
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
                <Button type="button" onClick={() => navigate("/pricing/pay")}>
                  {t("settings.subscription.confirm")}
                </Button>
              )}
              {(subscription.status === "ACTIVE" || subscription.status === "PENDING_PAYMENT") && (
                <Button type="button" variant="ghost" isLoading={isCanceling} onClick={onCancel}>
                  {t("settings.subscription.cancel")}
                </Button>
              )}
              {(subscription.status === "CANCELED" || subscription.status === "EXPIRED") && (
                <Button type="button" onClick={() => navigate("/pricing")}>
                  {t("settings.subscription.ctaPricing")}
                </Button>
              )}
            </div>

            <ErrorMessage message={actionError} size="sm" />
          </div>
        )}
      </CardState>
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
