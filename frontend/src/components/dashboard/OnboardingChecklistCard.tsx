import { Card } from "@/components/ui/Card";
import { LinkButton } from "@/components/ui/LinkButton";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";

interface Step {
  labelKey: MessageKey;
  ctaKey: MessageKey;
  to: string;
}

const STEPS: Step[] = [
  { labelKey: "dashboard.onboarding.step1", ctaKey: "dashboard.onboarding.step1Cta", to: "/" },
  { labelKey: "dashboard.onboarding.step2", ctaKey: "dashboard.onboarding.step2Cta", to: "/watchlist" },
  { labelKey: "dashboard.onboarding.step3", ctaKey: "dashboard.onboarding.step3Cta", to: "/pricing" },
];

export function OnboardingChecklistCard() {
  const { t } = useI18n();

  return (
    <Card title={t("dashboard.onboarding.title")} className="flex flex-col gap-4">
      <ol className="flex flex-col gap-4">
        {STEPS.map((step, index) => (
          <li key={step.labelKey} className="flex flex-wrap items-center gap-3">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-base border border-border font-mono text-xs text-text-dim">
              {index + 1}
            </span>
            <span className="flex-1 text-sm text-text">{t(step.labelKey)}</span>
            <LinkButton to={step.to} variant="ghost">
              {t(step.ctaKey)}
            </LinkButton>
          </li>
        ))}
      </ol>
    </Card>
  );
}
