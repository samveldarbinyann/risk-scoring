import { useState } from "react";
import { ApiKeyRow } from "@/components/settings/ApiKeyRow";
import { ApiKeySecretReveal } from "@/components/settings/ApiKeySecretReveal";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { CardState } from "@/components/ui/CardState";
import { EmptyState } from "@/components/ui/EmptyState";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Input } from "@/components/ui/Input";
import { useI18n } from "@/lib/i18n/context";
import type { ApiKeyCreatedView, ApiKeyView } from "@/lib/types";

interface ApiKeysPanelProps {
  keys: ApiKeyView[];
  isLoading: boolean;
  error: string | null;
  actionError: string | null;
  canCreate: boolean;
  isCreating: boolean;
  revokingId: string | null;
  createdSecret: ApiKeyCreatedView | null;
  onCreate: (name: string) => Promise<boolean>;
  onRevoke: (id: string) => void;
}

export function ApiKeysPanel({
  keys,
  isLoading,
  error,
  actionError,
  canCreate,
  isCreating,
  revokingId,
  createdSecret,
  onCreate,
  onRevoke,
}: ApiKeysPanelProps) {
  const { t } = useI18n();
  const [name, setName] = useState("");

  async function handleCreate() {
    const trimmed = name.trim();
    if (!trimmed || isCreating || !canCreate) return;
    const ok = await onCreate(trimmed);
    if (ok) setName("");
  }

  return (
    <Card title={t("settings.apiKeys.title")}>
      <div className="flex flex-col gap-6">
        {!canCreate && (
          <p className="font-mono text-sm text-text-dim">{t("settings.apiKeys.requiresActive")}</p>
        )}

        <div className="flex flex-col gap-3 sm:flex-row">
          <Input
            value={name}
            onChange={(event) => setName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") void handleCreate();
            }}
            placeholder={t("settings.apiKeys.namePlaceholder")}
            disabled={!canCreate || isCreating}
            maxLength={64}
            className="sm:flex-1"
          />
          <Button
            type="button"
            isLoading={isCreating}
            disabled={!canCreate || name.trim().length === 0}
            onClick={() => void handleCreate()}
            className="sm:w-auto"
          >
            {t("settings.apiKeys.create")}
          </Button>
        </div>

        {createdSecret && <ApiKeySecretReveal apiKey={createdSecret.apiKey} />}

        <ErrorMessage message={actionError} size="sm" />

        <CardState isLoading={isLoading} error={error}>
          {keys.length === 0 ? (
            <EmptyState message={t("settings.apiKeys.empty")} />
          ) : (
            <div>
              {keys.map((key) => (
                <ApiKeyRow
                  key={key.id}
                  apiKey={key}
                  isRevoking={revokingId === key.id}
                  onRevoke={onRevoke}
                />
              ))}
            </div>
          )}
        </CardState>
      </div>
    </Card>
  );
}
