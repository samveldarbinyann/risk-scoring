import { PlanFeatureList } from "@/components/pricing/PlanFeatureList";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/cn";
import { formatCount, formatMoney } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { POPULAR_PLAN } from "@/lib/plans";
import type { PlanView, SubscriptionView } from "@/lib/types";

export type PlanCtaKind = "signIn" | "select" | "pending" | "current" | "cancelFirst";

interface PlanCardProps {
  plan: PlanView;
  subscription: SubscriptionView | null;
  ctaKind: PlanCtaKind;
  isBusy: boolean;
  onSelect: (plan: PlanView) => void;
  onResumePayment: () => void;
  onSignIn: () => void;
}

export function PlanCard({
  plan,
  subscription,
  ctaKind,
  isBusy,
  onSelect,
  onResumePayment,
  onSignIn,
}: PlanCardProps) {
  const { t, locale } = useI18n();
  const isPopular = plan.code === POPULAR_PLAN;
  const isCurrent =
    subscription !== null &&
    subscription.planCode === plan.code &&
    (subscription.status === "ACTIVE" || subscription.status === "PENDING_PAYMENT");

  return (
    <article
      className={cn(
        "relative flex h-full flex-col gap-6 rounded-panel border bg-surface p-6",
        isPopular || isCurrent ? "border-accent" : "border-border",
      )}
    >
      {isPopular && (
        <span className="absolute -top-3 left-6 rounded-base border border-accent bg-bg px-3 py-1 font-mono text-xs uppercase tracking-wider text-accent">
          {t("pricing.popular")}
        </span>
      )}

      <header className="space-y-3">
        <h2 className="font-sans text-xs uppercase tracking-widest text-text-dim">
          {t(`pricing.plan.${plan.code}` as MessageKey)}
        </h2>
        <div className="flex items-baseline gap-2">
          <span className="font-mono text-4xl font-medium text-text">
            {formatMoney(plan.priceCents, plan.currency, locale)}
          </span>
          <span className="font-mono text-sm text-text-dim">{t("pricing.perMonth")}</span>
        </div>
        <p className="font-mono text-sm text-accent">
          {formatCount(plan.monthlyRequestLimit, locale)} {t("pricing.scansPerMonth")}
        </p>
      </header>

      <PlanFeatureList className="flex-1" />

      <PlanCta
        kind={ctaKind}
        isBusy={isBusy}
        onSelect={() => onSelect(plan)}
        onResumePayment={onResumePayment}
        onSignIn={onSignIn}
      />
    </article>
  );
}

interface PlanCtaProps {
  kind: PlanCtaKind;
  isBusy: boolean;
  onSelect: () => void;
  onResumePayment: () => void;
  onSignIn: () => void;
}

function PlanCta({ kind, isBusy, onSelect, onResumePayment, onSignIn }: PlanCtaProps) {
  const { t } = useI18n();

  switch (kind) {
    case "signIn":
      return (
        <Button
          type="button"
          variant="ghost"
          onClick={onSignIn}
          className="w-full whitespace-nowrap text-sm font-normal"
        >
          {t("pricing.cta.signIn")}
        </Button>
      );
    case "select":
      return (
        <Button type="button" isLoading={isBusy} onClick={onSelect} className="w-full">
          {t("pricing.cta.select")}
        </Button>
      );
    case "pending":
      return (
        <Button type="button" onClick={onResumePayment} className="w-full">
          {t("pricing.cta.pending")}
        </Button>
      );
    case "current":
      return (
        <Button type="button" disabled className="w-full">
          {t("pricing.cta.current")}
        </Button>
      );
    case "cancelFirst":
      return (
        <Button type="button" variant="ghost" disabled className="w-full whitespace-nowrap text-sm">
          {t("pricing.cta.cancelFirst")}
        </Button>
      );
  }
}
