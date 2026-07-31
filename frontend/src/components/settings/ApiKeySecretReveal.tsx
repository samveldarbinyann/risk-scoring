import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { useI18n } from "@/lib/i18n/context";

interface ApiKeySecretRevealProps {
  apiKey: string;
}

type CopyState = "idle" | "copied" | "failed";

export function ApiKeySecretReveal({ apiKey }: ApiKeySecretRevealProps) {
  const { t } = useI18n();
  const [copyState, setCopyState] = useState<CopyState>("idle");

  useEffect(() => {
    if (copyState === "idle") return;
    const timer = window.setTimeout(() => setCopyState("idle"), 2000);
    return () => window.clearTimeout(timer);
  }, [copyState]);

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(apiKey);
      setCopyState("copied");
    } catch {
      setCopyState("failed");
    }
  }

  const label =
    copyState === "copied"
      ? t("settings.apiKeys.copied")
      : copyState === "failed"
        ? t("settings.apiKeys.copyFailed")
        : t("settings.apiKeys.copy");

  return (
    <div className="space-y-3 rounded-panel border border-accent bg-surface-2 p-4">
      <div className="space-y-1">
        <p className="font-sans text-xs uppercase tracking-wider text-accent">{t("settings.apiKeys.secretTitle")}</p>
        <p className="font-mono text-xs text-text-dim">{t("settings.apiKeys.secretWarning")}</p>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <code className="min-w-0 flex-1 break-all rounded-base border border-border bg-bg px-4 py-3 font-mono text-sm text-text">
          {apiKey}
        </code>
        <Button type="button" variant="ghost" onClick={() => void handleCopy()} className="h-10 shrink-0 px-4 text-sm">
          {label}
        </Button>
      </div>
    </div>
  );
}
