import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { HeroInput } from "@/components/hero/HeroInput";
import { ChainSelect } from "@/components/hero/ChainSelect";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { createScan } from "@/lib/api";
import { isEvmAddress } from "@/lib/address";
import { EVM_CHAINS } from "@/lib/chains";
import { useI18n } from "@/lib/i18n/context";

export function LandingPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [address, setAddress] = useState("");
  const [chainId, setChainId] = useState(EVM_CHAINS[0].chainId);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingScanId, setPendingScanId] = useState<string | null>(null);
  const isLeaving = pendingScanId !== null;

  async function handleSubmit() {
    if (isSubmitting) return;

    const trimmedAddress = address.trim();
    if (!isEvmAddress(trimmedAddress)) {
      setError(t("landing.errorInvalidAddress"));
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const { scanId } = await createScan({ address: trimmedAddress, chainId });
      setPendingScanId(scanId);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorCreateFailed"));
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-8 px-6">
      <motion.div
        className="flex w-full max-w-2xl flex-col items-center gap-8 text-center"
        animate={isLeaving ? { opacity: 0, scale: 0.98 } : { opacity: 1, scale: 1 }}
        transition={{ duration: 0.35, ease: "easeInOut" }}
        onAnimationComplete={() => {
          if (isLeaving && pendingScanId) navigate(`/scan/${pendingScanId}`);
        }}
      >
        <div className="space-y-3">
          <p className="font-mono text-xs uppercase tracking-widest text-accent">Risk Scoring</p>
          <h1 className="font-sans text-3xl font-semibold text-balance text-text sm:text-4xl">
            {t("landing.title")}
          </h1>
          <p className="text-sm text-text-dim">{t("landing.subtitle")}</p>
        </div>

        <div className="flex w-full flex-col items-center gap-3">
          <HeroInput
            value={address}
            onChange={setAddress}
            onSubmit={handleSubmit}
            placeholder={t("landing.addressPlaceholder")}
            disabled={isSubmitting}
          />
          <div className="flex items-center gap-3">
            <ChainSelect value={chainId} onChange={setChainId} disabled={isSubmitting} />
            <Button
              onClick={handleSubmit}
              disabled={isSubmitting}
              aria-label={t("landing.scanButton")}
              aria-busy={isSubmitting}
            >
              {isSubmitting ? <Spinner /> : t("landing.scanButton")}
            </Button>
          </div>
          {error && <p className="font-mono text-xs text-risk-critical">{error}</p>}
        </div>
      </motion.div>
    </div>
  );
}
