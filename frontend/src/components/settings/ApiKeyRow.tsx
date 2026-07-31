import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/cn";
import { formatDateTime } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import type { ApiKeyStatus, ApiKeyView } from "@/lib/types";

const STATUS_CLASS: Record<ApiKeyStatus, string> = {
  ACTIVE: "border-accent text-accent",
  REVOKED: "border-border text-text-faint",
};

const STATUS_KEY: Record<ApiKeyStatus, MessageKey> = {
  ACTIVE: "settings.apiKeys.status.ACTIVE",
  REVOKED: "settings.apiKeys.status.REVOKED",
};

interface ApiKeyRowProps {
  apiKey: ApiKeyView;
  isRevoking: boolean;
  onRevoke: (id: string) => void;
}

export function ApiKeyRow({ apiKey, isRevoking, onRevoke }: ApiKeyRowProps) {
  const { t, locale } = useI18n();

  return (
    <div className="flex flex-col gap-3 border-b border-border py-4 last:border-b-0 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0 flex-1 space-y-2">
        <div className="flex flex-wrap items-center gap-3">
          <span className="font-sans text-sm text-text">{apiKey.name}</span>
          <span
            className={cn(
              "inline-flex items-center rounded-base border px-2.5 py-1 font-mono text-xs uppercase tracking-wider",
              STATUS_CLASS[apiKey.status],
            )}
          >
            {t(STATUS_KEY[apiKey.status])}
          </span>
        </div>
        <div className="flex flex-wrap gap-x-4 gap-y-1 font-mono text-xs text-text-dim">
          <span>
            {t("settings.apiKeys.prefix")}: {apiKey.keyPrefix}…
          </span>
          <span>
            {t("settings.apiKeys.created")}: {formatDateTime(apiKey.createdAt, locale)}
          </span>
          <span>
            {t("settings.apiKeys.lastUsed")}:{" "}
            {apiKey.lastUsedAt ? formatDateTime(apiKey.lastUsedAt, locale) : t("settings.apiKeys.neverUsed")}
          </span>
        </div>
      </div>
      {apiKey.status === "ACTIVE" && (
        <Button
          type="button"
          variant="ghost"
          isLoading={isRevoking}
          onClick={() => onRevoke(apiKey.id)}
          className="h-10 shrink-0 px-4 text-sm sm:self-center"
        >
          {t("settings.apiKeys.revoke")}
        </Button>
      )}
    </div>
  );
}
