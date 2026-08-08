import { Card } from "@/components/ui/Card";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { RISK_ORDER } from "@/lib/risk";

const STEP_KEYS: MessageKey[] = ["alerts.about.step1", "alerts.about.step2", "alerts.about.step3"];

export function AlertsAboutPanel() {
  const { t } = useI18n();

  return (
    <Card title={t("alerts.about.title")} className="flex flex-col gap-6">
      <ul className="flex flex-col gap-3">
        {STEP_KEYS.map((key) => (
          <li key={key} className="flex gap-3 text-sm text-text-dim">
            <span className="text-accent">&rsaquo;</span>
            <span>{t(key)}</span>
          </li>
        ))}
      </ul>

      <div className="flex flex-col gap-2 border-t border-border pt-5">
        <p className="font-sans text-xs uppercase tracking-wider text-text-faint">{t("alerts.about.legendTitle")}</p>
        <div className="flex flex-wrap gap-2">
          {RISK_ORDER.map((level) => (
            <RiskBadge key={level} level={level} />
          ))}
        </div>
      </div>
    </Card>
  );
}
