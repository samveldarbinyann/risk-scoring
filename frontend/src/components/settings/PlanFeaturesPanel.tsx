import { Card } from "@/components/ui/Card";
import { LinkButton } from "@/components/ui/LinkButton";
import { useI18n } from "@/lib/i18n/context";
import { PLAN_FEATURE_KEYS } from "@/lib/plans";

export function PlanFeaturesPanel() {
  const { t } = useI18n();

  return (
    <Card title={t("settings.features.title")} className="flex flex-col gap-6">
      <ul className="flex flex-col gap-3">
        {PLAN_FEATURE_KEYS.map((key) => (
          <li key={key} className="flex gap-3 text-sm text-text-dim">
            <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-base bg-accent" aria-hidden />
            <span>{t(key)}</span>
          </li>
        ))}
      </ul>

      <LinkButton to="/pricing" variant="ghost" className="w-fit">
        {t("settings.subscription.ctaPricing")}
      </LinkButton>
    </Card>
  );
}
