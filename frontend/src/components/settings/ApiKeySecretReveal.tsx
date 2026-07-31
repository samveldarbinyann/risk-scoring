import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { useI18n } from "@/lib/i18n/context";

interface ApiKeySecretRevealProps {
  apiKey: string;
}

export function ApiKeySecretReveal({ apiKey }: ApiKeySecretRevealProps) {
  const { t } = useI18n();
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(apiKey);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }

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
          {copied ? t("settings.apiKeys.copied") : t("settings.apiKeys.copy")}
        </Button>
      </div>
    </div>
  );
}
