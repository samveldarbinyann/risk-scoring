import { cn } from "@/lib/cn";
import { formatCount } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";

interface QuotaBarProps {
  used: number;
  limit: number;
  remaining: number;
}

export function QuotaBar({ used, limit, remaining }: QuotaBarProps) {
  const { t, locale } = useI18n();
  const ratio = limit > 0 ? Math.min(1, Math.max(0, used / limit)) : 0;
  const exhausted = remaining <= 0 && limit > 0;

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center justify-between gap-2 font-mono text-xs text-text-dim">
        <span>
          {t("settings.subscription.used")}: {formatCount(used, locale)}
        </span>
        <span>
          {t("settings.subscription.remaining")}: {formatCount(remaining, locale)}
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-base bg-surface-2">
        <div
          className={cn(
            "h-full origin-left rounded-base transition-transform",
            exhausted ? "bg-risk-critical" : "bg-accent",
          )}
          style={{ transform: `scaleX(${ratio})` }}
        />
      </div>
      <p className="font-mono text-xs text-text-faint">
        {t("settings.subscription.quota")}: {formatCount(used, locale)} / {formatCount(limit, locale)}
      </p>
    </div>
  );
}
