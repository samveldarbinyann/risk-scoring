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

export function SettingsPage() {
  const { t } = useI18n();
  const { status } = useAuth();

  const [subscription, setSubscription] = useState<SubscriptionView | null>(null);
  const [keys, setKeys] = useState<ApiKeyView[]>([]);
  const [subLoadError, setSubLoadError] = useState<string | null>(null);
  const [keysLoadError, setKeysLoadError] = useState<string | null>(null);
  const [subActionError, setSubActionError] = useState<string | null>(null);
  const [keysActionError, setKeysActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isConfirming, setIsConfirming] = useState(false);
  const [isCanceling, setIsCanceling] = useState(false);
  const [isCreatingKey, setIsCreatingKey] = useState(false);
  const [revokingId, setRevokingId] = useState<string | null>(null);
  const [createdSecret, setCreatedSecret] = useState<ApiKeyCreatedView | null>(null);

  const refresh = useCallback(async () => {
    setSubLoadError(null);
    setKeysLoadError(null);
    setIsLoading(true);

    const [subResult, keysResult] = await Promise.allSettled([
      getSubscription().catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 404) return null;
        throw err;
      }),
      listApiKeys(),
    ]);

    if (subResult.status === "fulfilled") {
      setSubscription(subResult.value);
    } else {
      setSubscription(null);
      setSubLoadError(
        subResult.reason instanceof Error ? subResult.reason.message : t("settings.subscription.loadError"),
      );
    }

    if (keysResult.status === "fulfilled") {
      setKeys(keysResult.value);
    } else {
      setKeys([]);
      setKeysLoadError(
        keysResult.reason instanceof Error ? keysResult.reason.message : t("settings.apiKeys.loadError"),
      );
    }

    setIsLoading(false);
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
    if (!subscription || isConfirming) return;
    setSubActionError(null);
    setIsConfirming(true);
    try {
      setSubscription(await confirmSubscriptionPayment(subscription.id));
    } catch (err) {
      setSubActionError(err instanceof Error ? err.message : t("settings.subscription.actionError"));
    } finally {
      setIsConfirming(false);
    }
  }

  async function handleCancel() {
    if (isCanceling) return;
    setSubActionError(null);
    setIsCanceling(true);
    try {
      setSubscription(await cancelSubscription());
    } catch (err) {
      setSubActionError(err instanceof Error ? err.message : t("settings.subscription.actionError"));
    } finally {
      setIsCanceling(false);
    }
  }

  async function handleCreateKey(name: string) {
    if (isCreatingKey) return;
    setKeysActionError(null);
    setIsCreatingKey(true);
    try {
      const created = await createApiKey({ name });
      setCreatedSecret(created);
      setKeys(await listApiKeys());
    } catch (err) {
      setKeysActionError(err instanceof Error ? err.message : t("settings.apiKeys.actionError"));
    } finally {
      setIsCreatingKey(false);
    }
  }

  async function handleRevokeKey(id: string) {
    if (revokingId) return;
    setKeysActionError(null);
    setRevokingId(id);
    try {
      await revokeApiKey(id);
      setKeys(await listApiKeys());
      if (createdSecret?.id === id) setCreatedSecret(null);
    } catch (err) {
      setKeysActionError(err instanceof Error ? err.message : t("settings.apiKeys.actionError"));
    } finally {
      setRevokingId(null);
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
        subscription={subscription}
        isLoading={isLoading}
        error={subLoadError}
        actionError={subActionError}
        isConfirming={isConfirming}
        isCanceling={isCanceling}
        onConfirm={() => void handleConfirm()}
        onCancel={() => void handleCancel()}
      />

      <ApiKeysPanel
        keys={keys}
        isLoading={isLoading}
        error={keysLoadError}
        actionError={keysActionError}
        canCreate={subscription?.status === "ACTIVE"}
        isCreating={isCreatingKey}
        revokingId={revokingId}
        createdSecret={createdSecret}
        onCreate={handleCreateKey}
        onRevoke={(id) => void handleRevokeKey(id)}
      />
    </div>
  );
}
