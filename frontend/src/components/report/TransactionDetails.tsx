import { TargetChip } from "@/components/ui/TargetChip";
import { nativeSymbol } from "@/lib/chains";
import { formatCount, formatDateTime, formatWei } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";
import { RISK } from "@/lib/risk";
import type { FlaggedExposure, TransactionEvidence } from "@/lib/types";

interface TransactionDetailsProps {
  chainId: number;
  evidence: TransactionEvidence;
}

export function TransactionDetails({ chainId, evidence }: TransactionDetailsProps) {
  const { t, locale } = useI18n();
  const statusClass = evidence.success ? RISK.LOW.text : RISK.CRITICAL.text;

  return (
    <div className="flex flex-col gap-6 p-6">
      <div>
        <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.txValue")}</p>
        <p className="mt-2 font-mono text-2xl text-text">
          {formatWei(evidence.valueWei, locale)} <span className="text-base text-text-dim">{nativeSymbol(chainId)}</span>
        </p>
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
          value={evidence.blockTimestamp === null ? "—" : formatDateTime(evidence.blockTimestamp, locale)}
        />
        <StatCell
          label={t("report.txInternalTransfers")}
          value={formatCount(evidence.internalTransferCount, locale)}
        />
        <StatCell label={t("report.txTokenTransfers")} value={formatCount(evidence.erc20TransferCount, locale)} />
      </div>

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
  address: string;
}

function PartyRow({ label, address }: PartyRowProps) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="font-sans text-xs uppercase tracking-wider text-text-dim">{label}</span>
      <TargetChip value={address} className="min-w-0 text-sm" />
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
