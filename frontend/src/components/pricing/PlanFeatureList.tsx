import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import { PLAN_FEATURE_KEYS } from "@/lib/plans";

interface PlanFeatureListProps {
  className?: string;
}

export function PlanFeatureList({ className }: PlanFeatureListProps) {
  const { t } = useI18n();

  return (
    <ul className={cn("flex flex-col gap-3", className)}>
      {PLAN_FEATURE_KEYS.map((key) => (
        <li key={key} className="flex gap-3 text-sm text-text-dim">
          <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-base bg-accent" aria-hidden />
          <span>{t(key)}</span>
        </li>
      ))}
    </ul>
  );
}
