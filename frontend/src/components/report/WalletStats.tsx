import { Card } from "@/components/ui/Card";
import { useI18n } from "@/lib/i18n/context";
import { nativeSymbol } from "@/lib/chains";
import { formatWei } from "@/lib/format";
import type { TokenBalance } from "@/lib/types";

interface WalletStatsProps {
  chainId: number;
  balanceWei: string;
  tokenBalances: TokenBalance[];
  txCount: number;
  txCount24h: number;
}

export function WalletStats({ chainId, balanceWei, tokenBalances, txCount, txCount24h }: WalletStatsProps) {
  const { t } = useI18n();

  return (
    <Card title={t("report.balance")}>
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-baseline gap-x-6 gap-y-2 font-mono">
          <span className="text-lg text-text">
            {formatWei(balanceWei)} {nativeSymbol(chainId)}
          </span>
          {tokenBalances.map((token) => (
            <span key={token.symbol} className="text-sm text-text-dim">
              {token.balanceFormatted} {token.symbol}
            </span>
          ))}
        </div>
        <div className="flex gap-6 font-mono text-sm text-text-dim">
          <span>
            {t("report.txCount")}: <span className="text-text">{txCount}</span>
          </span>
          <span>
            {t("report.txCount24h")}: <span className="text-text">{txCount24h}</span>
          </span>
        </div>
      </div>
    </Card>
  );
}
