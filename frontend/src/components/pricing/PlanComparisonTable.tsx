import { cn } from "@/lib/cn";
import { formatCount, formatMoney } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { POPULAR_PLAN } from "@/lib/plans";
import type { PlanView } from "@/lib/types";

interface PlanComparisonTableProps {
  plans: PlanView[];
}

interface ComparisonRow {
  labelKey: MessageKey;
  cell: (plan: PlanView, locale: string | undefined) => string;
}

function perScanPrice(plan: PlanView, locale: string | undefined): string {
  if (plan.monthlyRequestLimit <= 0) return "—";
  return formatMoney(plan.priceCents / plan.monthlyRequestLimit, plan.currency, locale);
}

const VALUE_ROWS: ComparisonRow[] = [
  {
    labelKey: "pricing.comparison.price",
    cell: (plan, locale) => formatMoney(plan.priceCents, plan.currency, locale),
  },
  {
    labelKey: "pricing.comparison.quota",
    cell: (plan, locale) => formatCount(plan.monthlyRequestLimit, locale),
  },
  {
    labelKey: "pricing.comparison.perScan",
    cell: (plan, locale) => perScanPrice(plan, locale),
  },
];

/** Identical across all plans today — table states that honestly rather than implying tiered perks. */
const INCLUDED_ROWS: MessageKey[] = [
  "pricing.comparison.apiAccess",
  "pricing.comparison.llmVerdict",
  "pricing.comparison.multiEvm",
  "pricing.comparison.watchlist",
];

export function PlanComparisonTable({ plans }: PlanComparisonTableProps) {
  const { t, locale } = useI18n();

  if (plans.length === 0) return null;

  return (
    <div className="space-y-4">
      <h2 className="text-center font-sans text-xs uppercase tracking-widest text-text-dim">
        {t("pricing.comparison.title")}
      </h2>
      <div className="overflow-x-auto rounded-panel border border-border">
        <table className="w-full min-w-140 border-collapse text-sm">
          <thead>
            <tr className="border-b border-border">
              <th className="p-4 text-left font-sans text-xs font-normal uppercase tracking-wider text-text-faint">
                {t("pricing.comparison.plan")}
              </th>
              {plans.map((plan) => (
                <th
                  key={plan.code}
                  className={cn(
                    "p-4 text-left font-sans text-xs font-normal uppercase tracking-wider",
                    plan.code === POPULAR_PLAN ? "text-accent" : "text-text-faint",
                  )}
                >
                  {t(`pricing.plan.${plan.code}` as MessageKey)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {VALUE_ROWS.map((row) => (
              <tr key={row.labelKey} className="border-b border-border last:border-0">
                <td className="p-4 font-sans text-text-dim">{t(row.labelKey)}</td>
                {plans.map((plan) => (
                  <td key={plan.code} className="p-4 font-mono text-text">
                    {row.cell(plan, locale)}
                  </td>
                ))}
              </tr>
            ))}
            {INCLUDED_ROWS.map((labelKey) => (
              <tr key={labelKey} className="border-b border-border last:border-0">
                <td className="p-4 font-sans text-text-dim">{t(labelKey)}</td>
                {plans.map((plan) => (
                  <td key={plan.code} className="p-4">
                    <span className="inline-block h-1.5 w-1.5 rounded-base bg-accent" aria-hidden />
                    <span className="sr-only">{t("pricing.comparison.included")}</span>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
