import { ApiEndpoint } from "@/components/docs/ApiEndpoint";
import { DocsSection } from "@/components/docs/DocsSection";
import { DocsTermList } from "@/components/docs/DocsTermList";
import { ChainIcon } from "@/components/ui/ChainIcon";
import { LinkButton } from "@/components/ui/LinkButton";
import { RiskBadge } from "@/components/ui/RiskBadge";
import { useChains } from "@/lib/chains/context";
import { useI18n } from "@/lib/i18n/context";
import { RISK_ORDER } from "@/lib/risk";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import type { DocsTerm } from "@/components/docs/DocsTermList";

const PIPELINE_STEPS: Array<{ titleKey: MessageKey; bodyKey: MessageKey }> = [
  { titleKey: "docs.pipeline.ingest.title", bodyKey: "docs.pipeline.ingest.body" },
  { titleKey: "docs.pipeline.enrich.title", bodyKey: "docs.pipeline.enrich.body" },
  { titleKey: "docs.pipeline.ai.title", bodyKey: "docs.pipeline.ai.body" },
  { titleKey: "docs.pipeline.stream.title", bodyKey: "docs.pipeline.stream.body" },
];

const SIGNAL_KEYS: MessageKey[] = [
  "docs.signals.mixer",
  "docs.signals.sanctions",
  "docs.signals.age",
  "docs.signals.fanInOut",
  "docs.signals.peelChain",
  "docs.signals.freshDrained",
  "docs.signals.roundAmounts",
];

const FLAG_CATEGORIES: Array<{ titleKey: MessageKey; bodyKey: MessageKey }> = [
  { titleKey: "docs.flags.sanctions.title", bodyKey: "docs.flags.sanctions.body" },
  { titleKey: "docs.flags.mixers.title", bodyKey: "docs.flags.mixers.body" },
  { titleKey: "docs.flags.exchanges.title", bodyKey: "docs.flags.exchanges.body" },
];

const SCAN_REQUEST_SAMPLE = `curl -X POST https://api.example.com/api/v1/scans \\
  -H "X-Api-Key: rsk_your_key_here" \\
  -H "Content-Type: application/json" \\
  -d '{"target":"0x742d35Cc6634C0532925a3b844Bc454e4438f44e","chains":["ETHEREUM","BNB_SMART_CHAIN"]}'

202 Accepted
{
  "groupId": "6f1c9b2e-...",
  "targetType": "ADDRESS",
  "target": "0x742d35cc6634c0532925a3b844bc454e4438f44e",
  "chains": ["ETHEREUM", "BNB_SMART_CHAIN"]
}`;

const REPORT_SAMPLE = `curl https://api.example.com/api/scans/groups/6f1c9b2e-.../report

200 OK
{
  "target": "0x742d35cc...",
  "reports": [
    {
      "chain": "ETHEREUM",
      "riskLevel": "HIGH",
      "score": 78,
      "explanation": "34% of inbound funds passed through a mixer ...",
      "decisiveSignals": ["mixer_exposure", "flagged_counterparty_1_hop"],
      "manualChecks": ["Verify the counterparty at 0x…"]
    }
  ]
}`;

const AUTH_SAMPLE = `X-Api-Key: rsk_live_9f2c...`;

export function DocsPage() {
  const { t } = useI18n();
  const { chains } = useChains();

  function toTerms(items: Array<{ titleKey: MessageKey; bodyKey: MessageKey }>): DocsTerm[] {
    return items.map((item) => ({ term: t(item.titleKey), description: t(item.bodyKey) }));
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-10 px-6 py-10">
      <header className="flex flex-col gap-3">
        <h1 className="font-sans text-3xl font-semibold text-text sm:text-4xl">{t("docs.title")}</h1>
        <p className="max-w-2xl text-sm leading-relaxed text-accent">{t("docs.subtitle")}</p>
      </header>

      <DocsSection index={1} title={t("docs.pipeline.title")} body={t("docs.pipeline.body")}>
        <DocsTermList numbered items={toTerms(PIPELINE_STEPS)} />
      </DocsSection>

      <DocsSection index={2} title={t("docs.risk.title")} body={t("docs.risk.body")}>
        <dl className="flex flex-col gap-3">
          {RISK_ORDER.map((level) => (
            <div
              key={level}
              className="flex flex-col gap-2 rounded-panel border border-border bg-surface p-5 sm:flex-row sm:items-baseline sm:gap-5"
            >
              <dt className="shrink-0">
                <RiskBadge level={level} />
              </dt>
              <dd className="text-sm leading-relaxed text-text-dim">{t(`docs.risk.${level}` as MessageKey)}</dd>
            </div>
          ))}
        </dl>
      </DocsSection>

      <DocsSection index={3} title={t("docs.signals.title")} body={t("docs.signals.body")}>
        <ul className="flex flex-col gap-2">
          {SIGNAL_KEYS.map((key) => (
            <li key={key} className="flex items-baseline gap-3 font-mono text-sm text-text-dim">
              <span className="text-accent">&rsaquo;</span>
              {t(key)}
            </li>
          ))}
        </ul>
      </DocsSection>

      <DocsSection index={4} title={t("docs.flags.title")} body={t("docs.flags.body")}>
        <DocsTermList items={toTerms(FLAG_CATEGORIES)} />
      </DocsSection>

      <DocsSection index={5} title={t("docs.api.title")} body={t("docs.api.body")}>
        <div className="flex flex-col gap-8">
          <ApiEndpoint title={t("docs.api.authTitle")} description={t("docs.api.authBody")} code={AUTH_SAMPLE} />
          <ApiEndpoint
            title={t("docs.api.scanTitle")}
            description={t("docs.api.scanBody")}
            code={SCAN_REQUEST_SAMPLE}
          />
          <ApiEndpoint title={t("docs.api.reportTitle")} description={t("docs.api.reportBody")} code={REPORT_SAMPLE} />
          <div className="flex flex-col gap-2">
            <h3 className="font-sans text-sm font-medium text-text">{t("docs.api.quotaTitle")}</h3>
            <p className="max-w-2xl text-sm leading-relaxed text-text-dim">{t("docs.api.quotaBody")}</p>
          </div>
        </div>
      </DocsSection>

      <DocsSection index={6} title={t("docs.networks.title")} body={t("docs.networks.body")}>
        <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {chains.map((chain) => (
            <li
              key={chain.chain}
              className="flex items-center gap-3 rounded-panel border border-border bg-surface px-4 py-3"
            >
              <span className="flex h-5 w-5 shrink-0 items-center justify-center text-text-dim">
                <ChainIcon chain={chain.chain} className="h-5 w-5" />
              </span>
              <span className="flex-1 font-sans text-sm text-text">{chain.displayName}</span>
              {!chain.mainnet && (
                <span className="rounded-base border border-border px-2 py-0.5 font-mono text-xs text-text-faint">
                  {t("docs.networks.testnet")}
                </span>
              )}
              <span className="font-mono text-xs text-text-faint">
                {chain.support === "SUPPORTED" ? t("docs.networks.statusLive") : t("docs.networks.statusSoon")}
              </span>
            </li>
          ))}
        </ul>
        <p className="max-w-2xl text-sm leading-relaxed text-text-dim">{t("docs.networks.roadmap")}</p>
      </DocsSection>

      <section className="flex flex-col gap-4 border-t border-border pt-8 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-1">
          <h2 className="font-sans text-lg font-semibold text-text">{t("docs.cta.title")}</h2>
          <p className="text-sm text-text-dim">{t("docs.cta.body")}</p>
        </div>
        <div className="flex shrink-0 gap-3">
          <LinkButton to="/pricing">{t("docs.cta.pricing")}</LinkButton>
          <LinkButton to="/contact" variant="ghost">
            {t("docs.cta.contact")}
          </LinkButton>
        </div>
      </section>
    </div>
  );
}
