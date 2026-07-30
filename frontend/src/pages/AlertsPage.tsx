import { useCallback, useEffect, useState } from "react";
import { Navigate } from "react-router";
import { AlertRow } from "@/components/alerts/AlertRow";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import { listAlerts } from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import type { AlertView } from "@/lib/types";

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
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-6 py-10">
      <header className="flex items-center justify-between gap-3">
        <h1 className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("alerts.title")}</h1>
        <Button type="button" variant="ghost" onClick={() => void refresh()} className="h-10 px-4 text-sm">
          {t("alerts.refresh")}
        </Button>
      </header>

      <Card>
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : error ? (
          <ErrorMessage message={error} size="sm" />
        ) : alerts.length === 0 ? (
          <p className="font-mono text-sm text-text-faint">{t("alerts.empty")}</p>
        ) : (
          <div>
            {alerts.map((alert) => (
              <AlertRow key={alert.id} alert={alert} />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
