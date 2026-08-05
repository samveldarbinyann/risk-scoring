import { useCallback, useEffect, useState } from "react";
import { motion, type Variants } from "motion/react";
import { Navigate } from "react-router";
import { ActivityLogCard } from "@/components/dashboard/ActivityLogCard";
import { PortfolioHeroCard } from "@/components/dashboard/PortfolioHeroCard";
import { QuotaSummaryCard } from "@/components/dashboard/QuotaSummaryCard";
import { WatchlistSummaryCard } from "@/components/dashboard/WatchlistSummaryCard";
import { Spinner } from "@/components/ui/Spinner";
import { ApiError, getRecentScans, getSubscription, listAlerts, listWatchlist } from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import type { AlertView, RecentScanGroupView, SubscriptionView, WatchlistEntryView } from "@/lib/types";

interface ResourceState<T> {
  data: T;
  error: string | null;
}

const GRID_VARIANTS: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};

const SECTION_VARIANTS: Variants = {
  hidden: { opacity: 0, y: 4 },
  show: { opacity: 1, y: 0, transition: { duration: 0.18, ease: "easeOut" } },
};

export function DashboardPage() {
  const { t } = useI18n();
  const { status } = useAuth();

  const [watchlist, setWatchlist] = useState<ResourceState<WatchlistEntryView[]>>({ data: [], error: null });
  const [alerts, setAlerts] = useState<ResourceState<AlertView[]>>({ data: [], error: null });
  const [subscription, setSubscription] = useState<ResourceState<SubscriptionView | null>>({
    data: null,
    error: null,
  });
  const [recentScans, setRecentScans] = useState<ResourceState<RecentScanGroupView[]>>({ data: [], error: null });
  const [isLoading, setIsLoading] = useState(true);

  const load = useCallback(async () => {
    setIsLoading(true);

    const [watchlistResult, alertsResult, subscriptionResult, recentScansResult] = await Promise.allSettled([
      listWatchlist(),
      listAlerts(),
      getSubscription().catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 404) return null;
        throw err;
      }),
      getRecentScans(),
    ]);

    setWatchlist(
      watchlistResult.status === "fulfilled"
        ? { data: watchlistResult.value, error: null }
        : { data: [], error: watchlistResult.reason instanceof Error ? watchlistResult.reason.message : t("watchlist.loadError") },
    );

    setAlerts(
      alertsResult.status === "fulfilled"
        ? { data: alertsResult.value, error: null }
        : { data: [], error: alertsResult.reason instanceof Error ? alertsResult.reason.message : t("alerts.loadError") },
    );

    setSubscription(
      subscriptionResult.status === "fulfilled"
        ? { data: subscriptionResult.value, error: null }
        : {
            data: null,
            error:
              subscriptionResult.reason instanceof Error
                ? subscriptionResult.reason.message
                : t("settings.subscription.loadError"),
          },
    );

    setRecentScans(
      recentScansResult.status === "fulfilled"
        ? { data: recentScansResult.value, error: null }
        : {
            data: [],
            error: recentScansResult.reason instanceof Error ? recentScansResult.reason.message : t("dashboard.recentScans.loadError"),
          },
    );

    setIsLoading(false);
  }, [t]);

  useEffect(() => {
    if (status !== "authenticated") return;
    void load();
  }, [status, load]);

  if (status === "loading") {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center px-6 py-10">
        <Spinner />
      </div>
    );
  }

  if (status === "unauthenticated") {
    return <Navigate to="/auth" replace />;
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-6 py-10">
      <h1 className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("dashboard.title")}</h1>

      <motion.div variants={GRID_VARIANTS} initial="hidden" animate="show" className="flex flex-col gap-6">
        <motion.div variants={SECTION_VARIANTS}>
          <PortfolioHeroCard entries={watchlist.data} isLoading={isLoading} error={watchlist.error} />
        </motion.div>

        <motion.div variants={SECTION_VARIANTS} className="grid gap-6 sm:grid-cols-2">
          <WatchlistSummaryCard entries={watchlist.data} isLoading={isLoading} error={watchlist.error} />
          <QuotaSummaryCard subscription={subscription.data} isLoading={isLoading} error={subscription.error} />
        </motion.div>

        <motion.div variants={SECTION_VARIANTS}>
          <ActivityLogCard
            scans={recentScans.data}
            alerts={alerts.data}
            isLoading={isLoading}
            error={recentScans.error ?? alerts.error}
          />
        </motion.div>
      </motion.div>
    </div>
  );
}
