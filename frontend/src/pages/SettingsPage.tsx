import { useCallback, useEffect, useState } from "react";
import { Navigate } from "react-router";
import { ApiKeysPanel } from "@/components/settings/ApiKeysPanel";
import { SubscriptionPanel } from "@/components/settings/SubscriptionPanel";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import {
  ApiError,
  cancelSubscription,
  confirmSubscriptionPayment,
  createApiKey,
  getSubscription,
  listApiKeys,
  revokeApiKey,
} from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import type { ApiKeyCreatedView, ApiKeyView, SubscriptionView } from "@/lib/types";

type ResourceState<T> = {
  data: T;
  loadError: string | null;
  actionError: string | null;
};

type BusyState = {
  loading: boolean;
  confirming: boolean;
  canceling: boolean;
  creatingKey: boolean;
  revokingId: string | null;
};

const INITIAL_BUSY: BusyState = {
  loading: true,
  confirming: false,
  canceling: false,
  creatingKey: false,
  revokingId: null,
};

export function SettingsPage() {
  const { t } = useI18n();
  const { status } = useAuth();

  const [subscription, setSubscription] = useState<ResourceState<SubscriptionView | null>>({
    data: null,
    loadError: null,
    actionError: null,
  });
  const [keys, setKeys] = useState<ResourceState<ApiKeyView[]>>({
    data: [],
    loadError: null,
    actionError: null,
  });
  const [busy, setBusy] = useState<BusyState>(INITIAL_BUSY);
  const [createdSecret, setCreatedSecret] = useState<ApiKeyCreatedView | null>(null);

  const refresh = useCallback(async () => {
    setSubscription((prev) => ({ ...prev, loadError: null, actionError: null }));
    setKeys((prev) => ({ ...prev, loadError: null, actionError: null }));
    setBusy((prev) => ({ ...prev, loading: true }));

    const [subResult, keysResult] = await Promise.allSettled([
      getSubscription().catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 404) return null;
        throw err;
      }),
      listApiKeys(),
    ]);

    if (subResult.status === "fulfilled") {
      setSubscription((prev) => ({ ...prev, data: subResult.value, loadError: null }));
    } else {
      setSubscription((prev) => ({
        ...prev,
        data: null,
        loadError:
          subResult.reason instanceof Error
            ? subResult.reason.message
            : t("settings.subscription.loadError"),
      }));
    }

    if (keysResult.status === "fulfilled") {
      setKeys((prev) => ({ ...prev, data: keysResult.value, loadError: null }));
    } else {
      setKeys((prev) => ({
        ...prev,
        data: [],
        loadError:
          keysResult.reason instanceof Error
            ? keysResult.reason.message
            : t("settings.apiKeys.loadError"),
      }));
    }

    setBusy((prev) => ({ ...prev, loading: false }));
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

  async function handleConfirm() {
    if (!subscription.data || busy.confirming) return;
    setSubscription((prev) => ({ ...prev, actionError: null }));
    setBusy((prev) => ({ ...prev, confirming: true }));
    try {
      const next = await confirmSubscriptionPayment(subscription.data.id);
      setSubscription((prev) => ({ ...prev, data: next }));
    } catch (err) {
      setSubscription((prev) => ({
        ...prev,
        actionError: err instanceof Error ? err.message : t("settings.subscription.actionError"),
      }));
    } finally {
      setBusy((prev) => ({ ...prev, confirming: false }));
    }
  }

  async function handleCancel() {
    if (busy.canceling) return;
    if (!window.confirm(t("settings.subscription.cancelConfirm"))) return;

    setSubscription((prev) => ({ ...prev, actionError: null }));
    setBusy((prev) => ({ ...prev, canceling: true }));
    try {
      const next = await cancelSubscription();
      const nextKeys = await listApiKeys();
      setSubscription((prev) => ({ ...prev, data: next }));
      setKeys((prev) => ({ ...prev, data: nextKeys }));
      setCreatedSecret(null);
    } catch (err) {
      setSubscription((prev) => ({
        ...prev,
        actionError: err instanceof Error ? err.message : t("settings.subscription.actionError"),
      }));
    } finally {
      setBusy((prev) => ({ ...prev, canceling: false }));
    }
  }

  async function handleCreateKey(name: string): Promise<boolean> {
    if (busy.creatingKey) return false;
    setKeys((prev) => ({ ...prev, actionError: null }));
    setBusy((prev) => ({ ...prev, creatingKey: true }));
    try {
      const created = await createApiKey({ name });
      const nextKeys = await listApiKeys();
      setCreatedSecret(created);
      setKeys((prev) => ({ ...prev, data: nextKeys }));
      return true;
    } catch (err) {
      setKeys((prev) => ({
        ...prev,
        actionError: err instanceof Error ? err.message : t("settings.apiKeys.actionError"),
      }));
      return false;
    } finally {
      setBusy((prev) => ({ ...prev, creatingKey: false }));
    }
  }

  async function handleRevokeKey(id: string) {
    if (busy.revokingId) return;
    if (!window.confirm(t("settings.apiKeys.revokeConfirm"))) return;

    setKeys((prev) => ({ ...prev, actionError: null }));
    setBusy((prev) => ({ ...prev, revokingId: id }));
    try {
      await revokeApiKey(id);
      const nextKeys = await listApiKeys();
      setKeys((prev) => ({ ...prev, data: nextKeys }));
      if (createdSecret?.id === id) setCreatedSecret(null);
    } catch (err) {
      setKeys((prev) => ({
        ...prev,
        actionError: err instanceof Error ? err.message : t("settings.apiKeys.actionError"),
      }));
    } finally {
      setBusy((prev) => ({ ...prev, revokingId: null }));
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-6 py-10">
      <header className="flex items-center justify-between gap-3">
        <h1 className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("settings.title")}</h1>
        <Button type="button" variant="ghost" onClick={() => void refresh()} className="h-10 px-4 text-sm">
          {t("settings.refresh")}
        </Button>
      </header>

      <SubscriptionPanel
        subscription={subscription.data}
        isLoading={busy.loading}
        error={subscription.loadError}
        actionError={subscription.actionError}
        isConfirming={busy.confirming}
        isCanceling={busy.canceling}
        onConfirm={() => void handleConfirm()}
        onCancel={() => void handleCancel()}
      />

      <ApiKeysPanel
        keys={keys.data}
        isLoading={busy.loading}
        error={keys.loadError}
        actionError={keys.actionError}
        canCreate={subscription.data?.status === "ACTIVE"}
        isCreating={busy.creatingKey}
        revokingId={busy.revokingId}
        createdSecret={createdSecret}
        onCreate={handleCreateKey}
        onRevoke={(id) => void handleRevokeKey(id)}
      />
    </div>
  );
}
