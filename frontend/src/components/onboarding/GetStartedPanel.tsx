import { motion } from "motion/react";
import { LinkButton } from "@/components/ui/LinkButton";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { useI18n } from "@/lib/i18n/context";
import type { RiskLevel } from "@/lib/types";

interface Example {
  address: string;
  chain: "ETHEREUM" | "BITCOIN" | "SOLANA";
  riskLevel: RiskLevel;
  score: number;
  reason: string;
}

const EXAMPLES: Example[] = [
  {
    address: "0x5d4F15F2b6Cb58c8c312d17F3Ff56eCea0b41d1C",
    chain: "ETHEREUM",
    riskLevel: "HIGH",
    score: 78,
    reason: "Linked to Tornado Cash mixer, 42% volume through mixing service",
  },
  {
    address: "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
    chain: "BITCOIN",
    riskLevel: "LOW",
    score: 18,
    reason: "New wallet, no flagged connections, standard transaction pattern",
  },
  {
    address: "9B5X6z9z9z9z9z9z9z9z9z9z9z9z9z9z9z9z9z9z9",
    chain: "SOLANA",
    riskLevel: "MEDIUM",
    score: 52,
    reason: "One hop from exchange wallet, low activity, standard pattern",
  },
];

interface GetStartedPanelProps {
  ctaTo?: string;
}

export function GetStartedPanel({ ctaTo = "/" }: GetStartedPanelProps) {
  const { t } = useI18n();

  const example = EXAMPLES[Math.floor(Math.random() * EXAMPLES.length)];

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="flex flex-col gap-8"
    >
      <div className="flex flex-col gap-3">
        <h2 className="font-sans text-xl font-semibold text-text">
          {t("onboarding.title")}
        </h2>
        <p className="text-sm leading-relaxed text-text-dim">
          {t("onboarding.subtitle")}
        </p>
      </div>

      <div className="rounded-panel border border-border bg-surface-2 p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ChainIcon chain={example.chain} className="h-4 w-4 text-text-dim" />
            <span className="text-xs font-mono text-text-faint uppercase tracking-wider">
              {t("onboarding.example")}
            </span>
          </div>
          <RiskBadge level={example.riskLevel} />
        </div>

        <div className="space-y-2">
          <div className="flex items-baseline gap-2">
            <span className="font-mono text-xs text-text-faint">score:</span>
            <span className="font-mono text-sm font-semibold text-text">
              {example.score}
            </span>
          </div>
          <p className="font-mono text-xs text-text-faint break-all">
            {example.address}
          </p>
        </div>

        <div className="border-t border-border pt-3">
          <p className="text-xs leading-relaxed text-text-dim">
            {example.reason}
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <LinkButton to={ctaTo} variant="primary" className="w-full">
          {t("onboarding.scanNow")}
        </LinkButton>
        <LinkButton to="/watchlist" variant="ghost" className="w-full">
          {t("onboarding.addToWatchlist")}
        </LinkButton>
      </div>

      <p className="text-xs text-text-faint leading-relaxed">
        {t("onboarding.hint")}
      </p>
    </motion.div>
  );
}