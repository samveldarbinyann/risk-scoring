import { PlanFeatureList } from "@/components/pricing/PlanFeatureList";
import { Card } from "@/components/ui/Card";
import { LinkButton } from "@/components/ui/LinkButton";
import { useI18n } from "@/lib/i18n/context";

export function PlanFeaturesPanel() {
  const { t } = useI18n();

  return (
    <Card title={t("settings.features.title")} className="flex flex-col gap-6">
      <PlanFeatureList />

      <LinkButton to="/pricing" variant="ghost" className="w-fit">
        {t("settings.subscription.ctaPricing")}
      </LinkButton>
    </Card>
  );
}
