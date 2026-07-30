import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { HeroInput } from "@/components/hero/HeroInput";
import { ChainPicker } from "@/components/hero/ChainPicker";
import { TypewriterText } from "@/components/ui/TypewriterText";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { typewriterDurationMs } from "@/hooks/useTypewriter";
import { createScan, getChainCandidates } from "@/lib/api";
import { isEvmAddress } from "@/lib/address";
import { useI18n } from "@/lib/i18n/context";
import type { ChainCandidatesResponse } from "@/lib/types";

const CROSSFADE = { duration: 0.35, ease: "easeInOut" } as const;
const FADED = { opacity: 0, scale: 0.98 };
const SHOWN = { opacity: 1, scale: 1 };
const PANEL = {
  initial: FADED,
  animate: SHOWN,
  exit: FADED,
  transition: CROSSFADE,
  className: "flex w-full flex-col items-center gap-3",
};

export function LandingPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);
  const [busyChainId, setBusyChainId] = useState<number | null>(null);
  const [candidates, setCandidates] = useState<ChainCandidatesResponse | null>(null);
  const [startedGroupId, setStartedGroupId] = useState<string | null>(null);

  async function handleSubmit() {
    if (isDetecting) return;

    const trimmedAddress = address.trim();
    if (!isEvmAddress(trimmedAddress)) {
      setError(t("landing.errorInvalidAddress"));
      return;
    }

    setError(null);
    setIsDetecting(true);
    try {
      setCandidates(await getChainCandidates(trimmedAddress));
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorChainsFailed"));
    } finally {
      setIsDetecting(false);
    }
  }

  async function handleSelectChain(chainId: number) {
    if (!candidates) return;

    setError(null);
    setBusyChainId(chainId);
    try {
      const { groupId } = await createScan({ address: candidates.address, chainIds: [chainId] });
      setStartedGroupId(groupId);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorCreateFailed"));
      setBusyChainId(null);
    }
  }

  function handleChangeAddress() {
    setCandidates(null);
    setError(null);
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-8 px-6">
      <motion.div
        className="relative flex w-full max-w-4xl flex-col items-center gap-3 text-center"
        animate={startedGroupId ? FADED : SHOWN}
        transition={CROSSFADE}
        onAnimationComplete={() => {
          if (startedGroupId) navigate(`/scan/${startedGroupId}`);
        }}
      >
        <AnimatePresence mode="wait">
          {candidates ? (
            <motion.div key="selecting" {...PANEL}>
              <TypewriterText
                as="h1"
                text={t("landing.chainPickerTitle")}
                className="min-h-9 text-balance font-sans text-3xl font-semibold text-text sm:min-h-14 sm:text-4xl"
              />
              <ChainPicker
                address={candidates.address}
                chainIds={candidates.chainIds}
                busyChainId={busyChainId}
                onSelect={handleSelectChain}
                onChangeAddress={handleChangeAddress}
              />
            </motion.div>
          ) : (
            <motion.div key="input" {...PANEL}>
              <div className="space-y-1">
                <p className="font-mono text-xs uppercase tracking-widest text-accent">Risk Scoring</p>
                <TypewriterText
                  as="h1"
                  text={t("landing.title")}
                  className="min-h-9 text-balance font-sans text-3xl font-semibold text-text sm:min-h-14 sm:text-4xl"
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
                  disabled={isDetecting}
                  submitLabel={t("landing.scanButton")}
                  isSubmitting={isDetecting}
                />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <ErrorMessage message={error} />
      </motion.div>
    </div>
  );
}
