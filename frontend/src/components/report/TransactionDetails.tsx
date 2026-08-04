import { NativeAmount } from "@/components/ui/NativeAmount";
import { TargetChip } from "@/components/ui/TargetChip";
import type { Chain } from "@/lib/chains/registry";
import {
  formatCount,
  formatDateTime,
  formatTokenAmount,
  isDisplayableSymbol,
  truncateId,
  UNKNOWN_VALUE,
} from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import { RISK } from "@/lib/risk";
import type { FlaggedExposure, TokenTransfer, TransactionEvidence } from "@/lib/types";

interface TransactionDetailsProps {
  chain: Chain;
  evidence: TransactionEvidence;
}

export function TransactionDetails({ chain, evidence }: TransactionDetailsProps) {
  const { t, locale } = useI18n();
  const statusClass = evidence.success ? RISK.LOW.text : RISK.CRITICAL.text;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div>
        <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.txValue")}</p>
        <NativeAmount chain={chain} raw={evidence.valueNative} />
      </div>

      <div className="flex flex-col gap-2">
        <PartyRow label={t("report.txFrom")} address={evidence.fromAddress} />
        <PartyRow label={t("report.txTo")} address={evidence.toAddress} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCell
          label={t("report.txStatus")}
          value={evidence.success ? t("report.txStatusSuccess") : t("report.txStatusFailed")}
          valueClassName={statusClass}
        />
        <StatCell
          label={t("report.txBlockTime")}
          value={evidence.blockTimestamp === null ? UNKNOWN_VALUE : formatDateTime(evidence.blockTimestamp, locale)}
        />
        <StatCell
          label={t("report.txInternalTransfers")}
          value={formatCount(evidence.nestedTransferCount, locale)}
        />
        <StatCell label={t("report.txTokenTransfers")} value={formatCount(evidence.tokenTransferCount, locale)} />
      </div>

      {evidence.tokenTransfers.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.txTokensMoved")}</p>
          {evidence.tokenTransfers.map((transfer, index) => (
            <TokenTransferRow key={`${transfer.contract}-${index}`} transfer={transfer} />
          ))}
        </div>
      )}

      <div className="flex flex-col gap-2">
        <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.txFlagged")}</p>
        {evidence.flagged.length === 0 ? (
          <p className="font-mono text-xs text-text-faint">{t("report.txNoFlagged")}</p>
        ) : (
          evidence.flagged.map((exposure) => <FlaggedRow key={`${exposure.address}-${exposure.hops}`} exposure={exposure} />)
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StatCell label={t("report.txParties")} value={formatCount(evidence.parties.length, locale)} />
        <StatCell label={t("report.observedAt")} value={formatDateTime(evidence.observedAt, locale)} />
      </div>
    </div>
  );
}

interface PartyRowProps {
  label: string;
  address: string | null;
}

function PartyRow({ label, address }: PartyRowProps) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="font-sans text-xs uppercase tracking-wider text-text-dim">{label}</span>
      {address === null ? (
        <span className="font-mono text-sm text-text-faint">{UNKNOWN_VALUE}</span>
      ) : (
        <TargetChip value={address} className="min-w-0 text-sm" />
      )}
    </div>
  );
}

interface TokenTransferRowProps {
  transfer: TokenTransfer;
}

function TokenTransferRow({ transfer }: TokenTransferRowProps) {
  const { locale } = useI18n();

  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="shrink-0 font-mono text-sm text-text">
        {formatTokenAmount(transfer.amount, locale)}{" "}
        <span className="text-text-dim">
          {transfer.symbol !== null && isDisplayableSymbol(transfer.symbol)
            ? transfer.symbol
            : truncateId(transfer.contract)}
        </span>
      </span>
      <span className="flex min-w-0 items-baseline gap-2 font-mono text-xs text-text-faint">
        <TargetChip value={transfer.from} className="min-w-0" />
        <span aria-hidden="true">→</span>
        <TargetChip value={transfer.to} className="min-w-0" />
      </span>
    </div>
  );
}

interface FlaggedRowProps {
  exposure: FlaggedExposure;
}

function FlaggedRow({ exposure }: FlaggedRowProps) {
  const toneClass = exposure.category === "SANCTION" ? RISK.CRITICAL.text : RISK.MEDIUM.text;

  return (
    <div className="flex items-baseline justify-between gap-4">
      <TargetChip value={exposure.address} className="min-w-0 text-sm" />
      <span className={`shrink-0 font-mono text-xs uppercase tracking-wider ${toneClass}`}>
        {exposure.label} · {exposure.hops}
      </span>
    </div>
  );
}

interface StatCellProps {
  label: string;
  value: string;
  valueClassName?: string;
}

function StatCell({ label, value, valueClassName }: StatCellProps) {
  return (
    <div className="flex flex-col gap-1">
      <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{label}</p>
      <p className={`font-mono text-sm ${valueClassName ?? "text-text"}`}>{value}</p>
    </div>
  );
}
