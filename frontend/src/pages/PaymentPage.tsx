import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { PaymentPendingPanel } from "@/components/pricing/PaymentPendingPanel";
import { LinkButton } from "@/components/ui/LinkButton";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import { ApiError, getSubscription } from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import type { SubscriptionView } from "@/lib/types";

export function PaymentPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { status } = useAuth();

  const [subscription, setSubscription] = useState<SubscriptionView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (status === "loading") return;
    if (status !== "authenticated") {
      navigate("/auth");
      return;
    }

    let cancelled = false;
    getSubscription()
      .then((sub) => {
        if (cancelled) return;
        if (sub.status !== "PENDING_PAYMENT" || !sub.paymentAddress) {
          navigate("/pricing");
          return;
        }
        setSubscription(sub);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          navigate("/pricing");
          return;
        }
        setLoadError(err instanceof Error ? err.message : t("pricing.loadError"));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [status, navigate, t]);

  function handleConfirmed() {
    navigate("/settings");
  }

  return (
    <div className="mx-auto flex w-full max-w-xl flex-1 flex-col justify-center gap-6 px-6 py-10">
      <LinkButton to="/pricing" variant="ghost" className="w-fit text-sm font-normal">
        {t("pricing.payment.backToPlans")}
      </LinkButton>

      {status === "loading" || isLoading ? (
        <div className="flex flex-1 items-center justify-center py-16">
          <Spinner />
        </div>
      ) : loadError ? (
        <ErrorMessage message={loadError} size="sm" />
      ) : (
        subscription && <PaymentPendingPanel subscription={subscription} onConfirmed={handleConfirmed} />
      )}
    </div>
  );
}
