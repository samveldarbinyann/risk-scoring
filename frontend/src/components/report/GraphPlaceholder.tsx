import { Card } from "@/components/ui/Card";
import { useI18n } from "@/lib/i18n/context";

export function GraphPlaceholder() {
  const { t } = useI18n();

  return (
    <Card title={t("report.graphTitle")}>
      <p className="text-sm text-text-dim">{t("report.graphPlaceholder")}</p>
    </Card>
  );
}
