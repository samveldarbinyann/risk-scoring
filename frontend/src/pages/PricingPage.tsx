import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { PlanCard, type PlanCtaKind } from "@/components/pricing/PlanCard";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import {
  activateSubscription,
  ApiError,
  confirmSubscriptionPayment,
  getSubscription,
  listPlans,
} from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import { PLAN_ORDER } from "@/lib/plans";
import type { PlanCode, PlanView, SubscriptionView } from "@/lib/types";

function resolveCtaKind(
  planCode: PlanCode,
  isAuthenticated: boolean,
  subscription: SubscriptionView | null,
): PlanCtaKind {
  if (!isAuthenticated) return "signIn";
  if (!subscription) return "select";

  const samePlan = subscription.planCode === planCode;

  if (subscription.status === "ACTIVE") {
    return samePlan ? "current" : "cancelFirst";
  }

  if (subscription.status === "PENDING_PAYMENT") {
    return samePlan ? "confirm" : "select";
  }

  return "select";
}

export function PricingPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { status } = useAuth();
  const isAuthenticated = status === "authenticated";

  const [plans, setPlans] = useState<PlanView[]>([]);
  const [subscription, setSubscription] = useState<SubscriptionView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyPlan, setBusyPlan] = useState<PlanCode | null>(null);

  const load = useCallback(async () => {
    setLoadError(null);
    setIsLoading(true);
    try {
      const planPromise = listPlans();
      if (!isAuthenticated) {
        setPlans(await planPromise);
        setSubscription(null);
        return;
      }

      const [nextPlans, nextSubscription] = await Promise.all([
        planPromise,
        getSubscription().catch((err: unknown) => {
          if (err instanceof ApiError && err.status === 404) return null;
          throw err;
        }),
      ]);
      setPlans(nextPlans);
      setSubscription(nextSubscription);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : t("pricing.loadError"));
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, t]);

  useEffect(() => {
    if (status === "loading") return;
    void load();
  }, [status, load]);

  const orderedPlans = useMemo(() => {
    const byCode = new Map(plans.map((plan) => [plan.code, plan]));
    return PLAN_ORDER.map((code) => byCode.get(code)).filter((plan): plan is PlanView => plan != null);
  }, [plans]);

  async function handleSelect(plan: PlanView) {
    if (busyPlan || !isAuthenticated) return;
    setActionError(null);
    setBusyPlan(plan.code);
    try {
      const next = await activateSubscription(plan.code);
      setSubscription(next);
      if (next.status === "ACTIVE") {
        navigate("/settings");
      }
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("pricing.activateError"));
    } finally {
      setBusyPlan(null);
    }
  }

  async function handleConfirm(subscriptionId: string) {
    if (busyPlan || !subscription) return;
    setActionError(null);
    setBusyPlan(subscription.planCode);
    try {
      const active = await confirmSubscriptionPayment(subscriptionId);
      setSubscription(active);
      navigate("/settings");
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("pricing.confirmError"));
    } finally {
      setBusyPlan(null);
    }
  }

  if (status === "loading" || isLoading) {
    return (
      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col items-center justify-center px-6 py-10">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center gap-10 px-6 py-10">
      <header className="mx-auto max-w-2xl space-y-3 text-center">
        <h1 className="font-sans text-3xl font-semibold text-text sm:text-4xl">{t("pricing.title")}</h1>
        <p className="text-sm text-accent">{t("pricing.subtitle")}</p>
      </header>

      {loadError ? (
        <ErrorMessage message={loadError} size="sm" />
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {orderedPlans.map((plan) => (
            <PlanCard
              key={plan.code}
              plan={plan}
              subscription={subscription}
              ctaKind={resolveCtaKind(plan.code, isAuthenticated, subscription)}
              isBusy={busyPlan === plan.code}
              onSelect={(selected) => void handleSelect(selected)}
              onConfirm={(id) => void handleConfirm(id)}
              onSignIn={() => navigate("/auth")}
            />
          ))}
        </div>
      )}

      <div className="flex justify-center">
        <ErrorMessage message={actionError} size="sm" />
      </div>
    </div>
  );
}
