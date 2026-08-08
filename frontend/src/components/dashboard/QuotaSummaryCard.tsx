import { QuotaBar } from "@/components/settings/QuotaBar";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { LinkButton } from "@/components/ui/LinkButton";
import { cn } from "@/lib/cn";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { SUBSCRIPTION_STATUS_CLASS, SUBSCRIPTION_STATUS_KEY } from "@/lib/subscriptionStatus";
import type { SubscriptionView } from "@/lib/types";

interface QuotaSummaryCardProps {
  subscription: SubscriptionView | null;
  isLoading: boolean;
  error: string | null;
}

export function QuotaSummaryCard({ subscription, isLoading, error }: QuotaSummaryCardProps) {
  const { t, locale } = useI18n();

  return (
    <Card title={t("dashboard.quota.title")} className="flex h-full flex-col">
      <CardState isLoading={isLoading} error={error}>
        {!subscription ? (
          <EmptyState
            message={t("dashboard.quota.empty")}
            ctaLabel={t("dashboard.quota.cta")}
            ctaTo="/pricing"
          />
        ) : (
          <div className="flex flex-1 flex-col gap-4">
            <div className="flex items-center justify-between gap-3">
              <span className="font-sans text-sm text-text">
                {t(`pricing.plan.${subscription.planCode}` as MessageKey)}
              </span>
              <span
                className={cn(
                  "inline-flex items-center rounded-base border px-2.5 py-1 font-mono text-xs uppercase tracking-wider",
                  SUBSCRIPTION_STATUS_CLASS[subscription.status],
                )}
              >
                {t(SUBSCRIPTION_STATUS_KEY[subscription.status])}
              </span>
            </div>

            {subscription.status === "ACTIVE" && (
              <QuotaBar
                used={subscription.requestsUsed}
                limit={subscription.monthlyRequestLimit}
                remaining={subscription.requestsRemaining}
              />
            )}

            {subscription.currentPeriodEnd && (
              <p className="font-mono text-xs text-text-faint">
                {t("dashboard.quota.resets")}: {formatDateTime(subscription.currentPeriodEnd, locale)}
              </p>
            )}

            <LinkButton to="/settings" variant="ghost" className="mt-auto w-fit">
              {t("settings.title")}
            </LinkButton>
          </div>
        )}
      </CardState>
    </Card>
  );
}
