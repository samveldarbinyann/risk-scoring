import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { getSubscription } from "@/lib/api";
import { useI18n } from "@/lib/i18n/context";
import { pollUntil } from "@/lib/poll";
import type { SubscriptionView } from "@/lib/types";

interface PaymentPendingPanelProps {
  subscription: SubscriptionView;
  onConfirmed: (subscription: SubscriptionView) => void;
}

type CopyField = "address" | "amount";

export function PaymentPendingPanel({ subscription, onConfirmed }: PaymentPendingPanelProps) {
  const { t } = useI18n();
  const [copied, setCopied] = useState<CopyField | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(() => remainingSeconds(subscription.paymentExpiresAt));
  const [isWaiting, setIsWaiting] = useState(false);
  const [showRetryHint, setShowRetryHint] = useState(false);

  useEffect(() => {
    setSecondsLeft(remainingSeconds(subscription.paymentExpiresAt));
    const timer = window.setInterval(() => {
      setSecondsLeft(remainingSeconds(subscription.paymentExpiresAt));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [subscription.paymentExpiresAt]);

  useEffect(() => {
    let cancelled = false;
    setIsWaiting(true);
    setShowRetryHint(false);

    const intervalMs = 5000;
    const remainingMs = remainingSeconds(subscription.paymentExpiresAt) * 1000;
    const maxAttempts = Math.max(1, Math.ceil(remainingMs / intervalMs) + 2);

    void pollUntil(getSubscription, (s) => s.status !== "PENDING_PAYMENT", {
      intervalMs,
      maxAttempts,
    }).then(({ value, matched }) => {
      if (cancelled) return;
      setIsWaiting(false);
      if (matched && value.status === "ACTIVE") {
        onConfirmed(value);
        return;
      }
      setShowRetryHint(true);
    });

    return () => {
      cancelled = true;
    };
  }, [subscription.id, subscription.paymentExpiresAt, onConfirmed]);

  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(null), 1500);
    return () => window.clearTimeout(timer);
  }, [copied]);

  async function handleCopy(field: CopyField, value: string) {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(field);
    } catch {
      // clipboard unavailable — nothing else to fall back to
    }
  }

  const address = subscription.paymentAddress ?? "";
  const amount = subscription.paymentAmount != null ? subscription.paymentAmount.toFixed(6) : "";
  const expired = secondsLeft <= 0;

  return (
    <section className="mx-auto w-full max-w-xl space-y-5 rounded-panel border border-accent bg-surface p-6">
      <h2 className="font-sans text-xs uppercase tracking-wider text-accent">{t("pricing.payment.title")}</h2>

      {expired ? (
        <p className="font-mono text-sm text-text-dim">{t("pricing.payment.expired")}</p>
      ) : (
        <>
          {subscription.paymentUri && (
            <div className="flex flex-col items-center gap-2">
              <div className="rounded-panel bg-white p-4">
                <QRCodeSVG value={subscription.paymentUri} size={200} />
              </div>
              <p className="font-mono text-xs text-text-dim">{t("pricing.payment.qrHint")}</p>
            </div>
          )}

          <PaymentField
            caption={t("pricing.payment.network")}
            value={t("pricing.payment.networkValue")}
            copyable={false}
          />

          <PaymentField
            caption={t("pricing.payment.addressLabel")}
            value={address}
            copyable
            copied={copied === "address"}
            onCopy={() => void handleCopy("address", address)}
            copyLabel={t("pricing.payment.copy")}
            copiedLabel={t("pricing.payment.copied")}
          />

          <PaymentField
            caption={t("pricing.payment.amountLabel")}
            value={amount}
            copyable
            copied={copied === "amount"}
            onCopy={() => void handleCopy("amount", amount)}
            copyLabel={t("pricing.payment.copy")}
            copiedLabel={t("pricing.payment.copied")}
            large
          />
          <p className="font-mono text-xs text-text-dim">{t("pricing.payment.amountHint")}</p>

          <div className="flex items-center justify-between border-t border-border pt-4 font-mono text-xs text-text-dim">
            <span>{t("pricing.payment.expiresInLabel")}</span>
            <span className="text-text">{formatCountdown(secondsLeft)}</span>
          </div>

          <div className="flex items-center gap-3 border-t border-border pt-4">
            {isWaiting && <Spinner />}
            <p className="font-mono text-sm text-text-dim">{t("pricing.payment.waiting")}</p>
          </div>

          {showRetryHint && <p className="font-mono text-xs text-text-dim">{t("pricing.payment.retryHint")}</p>}
        </>
      )}
    </section>
  );
}

interface PaymentFieldProps {
  caption: string;
  value: string;
  copyable: boolean;
  copied?: boolean;
  onCopy?: () => void;
  copyLabel?: string;
  copiedLabel?: string;
  large?: boolean;
}

function PaymentField({ caption, value, copyable, copied, onCopy, copyLabel, copiedLabel, large }: PaymentFieldProps) {
  return (
    <div className="space-y-1.5">
      <p className="font-sans text-xs uppercase tracking-wider text-text-dim">{caption}</p>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <code
          className={`min-w-0 flex-1 break-all rounded-base border border-border bg-bg px-4 py-3 font-mono text-text ${large ? "text-lg" : "text-sm"}`}
        >
          {value}
        </code>
        {copyable && (
          <Button type="button" variant="ghost" onClick={onCopy} className="h-10 shrink-0 px-4 text-sm">
            {copied ? copiedLabel : copyLabel}
          </Button>
        )}
      </div>
    </div>
  );
}

function remainingSeconds(expiresAt: string | null): number {
  if (!expiresAt) return 0;
  return Math.max(0, Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000));
}

function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}
