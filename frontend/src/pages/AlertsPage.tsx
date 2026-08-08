import { useCallback, useEffect, useState } from "react";
import { motion, type Variants } from "motion/react";
import { Navigate } from "react-router";
import { AlertRow } from "@/components/alerts/AlertRow";
import { AlertsAboutPanel } from "@/components/alerts/AlertsAboutPanel";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { listAlerts } from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import type { AlertView } from "@/lib/types";

const GRID_VARIANTS: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};

const SECTION_VARIANTS: Variants = {
  hidden: { opacity: 0, y: 4 },
  show: { opacity: 1, y: 0, transition: { duration: 0.18, ease: "easeOut" } },
};

export function AlertsPage() {
  const { t } = useI18n();
  const { status } = useAuth();
  const [alerts, setAlerts] = useState<AlertView[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refresh = useCallback(async () => {
    setError(null);
    setIsLoading(true);
    try {
      setAlerts(await listAlerts());
    } catch (err) {
      setError(err instanceof Error ? err.message : t("alerts.loadError"));
    } finally {
      setIsLoading(false);
    }
  }, [t]);

  useEffect(() => {
    if (status !== "authenticated") return;
    void refresh();
  }, [status, refresh]);

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
      <h1 className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("alerts.title")}</h1>

      <motion.div
        variants={GRID_VARIANTS}
        initial="hidden"
        animate="show"
        className="grid gap-6 lg:grid-cols-[1fr_320px]"
      >
        <motion.div variants={SECTION_VARIANTS}>
          <Card>
            <CardState isLoading={isLoading} error={error}>
              {alerts.length === 0 ? (
                <EmptyState
                  message={t("alerts.empty")}
                  hint={t("alerts.empty.hint")}
                  ctaLabel={t("dashboard.hero.emptyCta")}
                  ctaTo="/watchlist"
                />
              ) : (
                <div>
                  {alerts.map((alert) => (
                    <AlertRow key={alert.id} alert={alert} />
                  ))}
                </div>
              )}
            </CardState>
          </Card>
        </motion.div>

        <motion.div variants={SECTION_VARIANTS}>
          <AlertsAboutPanel />
        </motion.div>
      </motion.div>
    </div>
  );
}
