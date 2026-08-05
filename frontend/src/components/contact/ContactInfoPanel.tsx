import { Card } from "@/components/ui/Card";
import { LinkButton } from "@/components/ui/LinkButton";
import { useI18n } from "@/lib/i18n/context";

interface InfoItemProps {
  title: string;
  body: string;
}

function InfoItem({ title, body }: InfoItemProps) {
  return (
    <div className="flex flex-col gap-1">
      <p className="font-sans text-sm font-medium text-text">{title}</p>
      <p className="text-sm leading-relaxed text-text-dim">{body}</p>
    </div>
  );
}

export function ContactInfoPanel() {
  const { t } = useI18n();

  return (
    <Card title={t("contact.panel.eyebrow")} className="flex flex-col gap-6">
      <div className="flex flex-col gap-5">
        <InfoItem title={t("contact.panel.apiTitle")} body={t("contact.panel.apiBody")} />
        <InfoItem title={t("contact.panel.verdictTitle")} body={t("contact.panel.verdictBody")} />
        <InfoItem title={t("contact.panel.docsTitle")} body={t("contact.panel.docsBody")} />
      </div>

      <div className="flex flex-col gap-1 border-t border-border pt-5">
        <p className="font-sans text-xs uppercase tracking-wider text-text-faint">{t("contact.panel.responseTitle")}</p>
        <p className="font-mono text-sm text-text">{t("contact.responseTime")}</p>
      </div>

      <LinkButton to="/docs" variant="ghost" className="w-fit">
        {t("contact.panel.docsCta")}
      </LinkButton>
    </Card>
  );
}
