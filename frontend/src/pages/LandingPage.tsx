import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { HeroInput } from "@/components/hero/HeroInput";
import { ChainPicker } from "@/components/hero/ChainPicker";
import { TypewriterText } from "@/components/ui/TypewriterText";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { typewriterDurationMs } from "@/hooks/useTypewriter";
import { createScan, getChainCandidates } from "@/lib/api";
import { classifyTarget } from "@/lib/scanTarget";
import { useI18n } from "@/lib/i18n/context";
import { SCAN_FLOW_HIDDEN, SCAN_FLOW_TRANSITION, SCAN_FLOW_VISIBLE } from "@/lib/scanFlowMotion";
import type { ChainCandidatesResponse } from "@/lib/types";

const PANEL = {
  initial: SCAN_FLOW_HIDDEN,
  animate: SCAN_FLOW_VISIBLE,
  exit: SCAN_FLOW_HIDDEN,
  transition: SCAN_FLOW_TRANSITION,
  className: "flex w-full flex-col items-center gap-3",
};

export function LandingPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [target, setTarget] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);
  const [busyChainId, setBusyChainId] = useState<number | null>(null);
  const [candidates, setCandidates] = useState<ChainCandidatesResponse | null>(null);
  const [startedGroupId, setStartedGroupId] = useState<string | null>(null);

  async function handleSubmit() {
    if (isDetecting) return;

    const trimmedTarget = target.trim();
    if (classifyTarget(trimmedTarget) === null) {
      setError(t("landing.errorInvalidTarget"));
      return;
    }

    setError(null);
    setIsDetecting(true);
    try {
      setCandidates(await getChainCandidates(trimmedTarget));
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
      const { groupId } = await createScan({ target: candidates.target, chainIds: [chainId] });
      setStartedGroupId(groupId);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorCreateFailed"));
      setBusyChainId(null);
    }
  }

  function handleChangeTarget() {
    setCandidates(null);
    setError(null);
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-8 px-6">
      <motion.div
        className="relative flex w-full max-w-4xl flex-col items-center gap-3 text-center"
        animate={startedGroupId ? SCAN_FLOW_HIDDEN : SCAN_FLOW_VISIBLE}
        transition={SCAN_FLOW_TRANSITION}
        onAnimationComplete={() => {
          if (startedGroupId) navigate(`/scan/${startedGroupId}`);
        }}
      >
        <AnimatePresence mode="wait">
          {candidates ? (
            <motion.div key="selecting" {...PANEL}>
              <TypewriterText
                as="h1"
                text={
                  candidates.targetType === "TRANSACTION"
                    ? t("landing.chainPickerTitleTransaction")
                    : t("landing.chainPickerTitle")
                }
                className="min-h-9 text-balance font-sans text-3xl font-semibold text-text sm:min-h-14 sm:text-4xl"
              />
              <ChainPicker
                target={candidates.target}
                chainIds={candidates.chainIds}
                busyChainId={busyChainId}
                onSelect={handleSelectChain}
                onChangeTarget={handleChangeTarget}
              />
            </motion.div>
          ) : (
            <motion.div key="input" {...PANEL} className="flex w-full flex-col items-center gap-6">
              <div className="space-y-1">
                <TypewriterText
                  as="h1"
                  text={t("landing.title")}
                  className="min-h-9 text-balance font-sans text-3xl font-semibold text-text sm:min-h-14 sm:text-4xl"
                />
                <TypewriterText
                  as="p"
                  text={t("landing.subtitle")}
                  delayMs={typewriterDurationMs(t("landing.title")) + 150}
                  className="relative -top-3 min-h-5 text-sm text-accent"
                />
              </div>

              <div className="relative w-full max-w-2xl">
                <HeroInput
                  value={target}
                  onChange={setTarget}
                  onSubmit={handleSubmit}
                  placeholder={t("landing.targetPlaceholder")}
                  disabled={isDetecting}
                  submitLabel={t("landing.scanButton")}
                  isSubmitting={isDetecting}
                />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="h-4">
          <ErrorMessage message={error} />
        </div>
      </motion.div>
    </div>
  );
}
