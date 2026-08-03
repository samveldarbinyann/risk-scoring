import { useState } from "react";
import { nativeSymbol } from "@/lib/chains";
import { formatCount, formatDateTime, formatTokenAmount, formatUsd, formatWei, isDisplayableSymbol } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import type { TokenBalance } from "@/lib/types";

const VISIBLE_TOKEN_LIMIT = 5;

interface WalletStatsProps {
  chainId: number;
  balanceWei: string;
  tokenBalances: TokenBalance[];
  txCount: number;
  txCount24h: number;
  sampleTruncated: boolean;
  observedAt: string;
}

export function WalletStats({
  chainId,
  balanceWei,
  tokenBalances,
  txCount,
  txCount24h,
  sampleTruncated,
  observedAt,
}: WalletStatsProps) {
  const { t, locale } = useI18n();
  const [expanded, setExpanded] = useState(false);

  const displayableTokens = tokenBalances.filter((token) => isDisplayableSymbol(token.symbol));
  const hiddenCount = tokenBalances.length - displayableTokens.length;
  const sortedTokens = [...displayableTokens].sort((a, b) => (b.usdValue ?? -1) - (a.usdValue ?? -1));
  const visibleTokens = expanded ? sortedTokens : sortedTokens.slice(0, VISIBLE_TOKEN_LIMIT);
  const hasMoreTokens = sortedTokens.length > VISIBLE_TOKEN_LIMIT;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div>
        <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.balance")}</p>
        <p className="mt-2 font-mono text-2xl text-text">
          {formatWei(balanceWei, locale)} <span className="text-base text-text-dim">{nativeSymbol(chainId)}</span>
        </p>
      </div>

      {sortedTokens.length > 0 && (
        <div className="flex flex-col gap-2">
          {visibleTokens.map((token) => (
            <div key={token.symbol} className="flex items-baseline justify-between gap-4 font-mono text-sm">
              <span className="text-text-dim">{token.symbol}</span>
              <span className="text-text">
                {formatTokenAmount(token.balanceFormatted, locale)}
                {token.usdValue !== null && (
                  <span className="ml-2 text-text-faint">{formatUsd(token.usdValue, locale)}</span>
                )}
              </span>
            </div>
          ))}
          {hasMoreTokens && (
            <button
              type="button"
              onClick={() => setExpanded((value) => !value)}
              className="w-fit font-mono text-xs uppercase tracking-widest text-text-dim transition-colors hover:text-text"
            >
              {expanded ? t("report.showFewerTokens") : t("report.showMoreTokens")}
            </button>
          )}
        </div>
      )}

      {hiddenCount > 0 && (
        <p className="font-mono text-xs text-text-faint">
          {t("report.hiddenTokens")}: {hiddenCount}
        </p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCell
          label={t("report.transfers")}
          value={formatCount(txCount, locale)}
          hint={sampleTruncated ? t("report.sampleTruncated") : undefined}
        />
        <StatCell label={t("report.transfers24h")} value={formatCount(txCount24h, locale)} />
        <StatCell label={t("report.observedAt")} value={formatDateTime(observedAt, locale)} />
      </div>
    </div>
  );
}

interface StatCellProps {
  label: string;
  value: string;
  hint?: string;
}

function StatCell({ label, value, hint }: StatCellProps) {
  return (
    <div className="flex flex-col gap-1">
      <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{label}</p>
      <p className="font-mono text-sm text-text">{value}</p>
      {hint && <p className="font-mono text-xs text-risk-mid">{hint}</p>}
    </div>
  );
}
