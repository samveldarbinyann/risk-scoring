import { useNavigate } from "react-router";
import { QuotaBar } from "@/components/settings/QuotaBar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
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
  const navigate = useNavigate();

  return (
    <Card title={t("dashboard.quota.title")}>
      {isLoading ? (
        <div className="flex justify-center py-8">
          <Spinner />
        </div>
      ) : error ? (
        <ErrorMessage message={error} size="sm" />
      ) : !subscription ? (
        <div className="flex flex-col gap-4">
          <p className="font-mono text-sm text-text-faint">{t("dashboard.quota.empty")}</p>
          <Button type="button" variant="ghost" onClick={() => navigate("/pricing")} className="w-fit">
            {t("dashboard.quota.cta")}
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
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

          <Button type="button" variant="ghost" onClick={() => navigate("/settings")} className="w-fit">
            {t("settings.title")}
          </Button>
        </div>
      )}
    </Card>
  );
}
