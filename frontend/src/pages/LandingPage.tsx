import { useState } from "react";
import { useNavigate } from "react-router";
import { motion } from "motion/react";
import { HeroInput } from "@/components/hero/HeroInput";
import { TypewriterText } from "@/components/hero/TypewriterText";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { typewriterDurationMs } from "@/hooks/useTypewriter";
import { createScan } from "@/lib/api";
import { isEvmAddress } from "@/lib/address";
import { useI18n } from "@/lib/i18n/context";

export function LandingPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingGroupId, setPendingGroupId] = useState<string | null>(null);
  const isLeaving = pendingGroupId !== null;

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
      const { groupId } = await createScan({ address: trimmedAddress });
      setPendingGroupId(groupId);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorCreateFailed"));
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-8 px-6">
      <motion.div
        className="relative flex w-full max-w-2xl flex-col items-center gap-8 text-center"
        animate={isLeaving ? { opacity: 0, scale: 0.98 } : { opacity: 1, scale: 1 }}
        transition={{ duration: 0.35, ease: "easeInOut" }}
        onAnimationComplete={() => {
          if (isLeaving && pendingGroupId) navigate(`/scan/${pendingGroupId}`);
        }}
      >
        <div className="space-y-3">
          <p className="font-mono text-xs uppercase tracking-widest text-accent">Risk Scoring</p>
          <TypewriterText
            as="h1"
            text={t("landing.title")}
            className="whitespace-nowrap font-sans text-3xl font-semibold text-text sm:text-4xl"
          />
          <TypewriterText
            as="p"
            text={t("landing.subtitle")}
            delayMs={typewriterDurationMs(t("landing.title")) + 150}
            className="text-sm text-text-dim"
          />
        </div>

        <div className="relative w-full max-w-2xl">
          <HeroInput
            value={address}
            onChange={setAddress}
            onSubmit={handleSubmit}
            placeholder={t("landing.addressPlaceholder")}
            disabled={isSubmitting}
            submitLabel={t("landing.scanButton")}
            isSubmitting={isSubmitting}
          />
          <div className="absolute inset-x-0 top-full mt-3 flex justify-center">
            <ErrorMessage message={error} />
          </div>
        </div>
      </motion.div>
    </div>
  );
}
