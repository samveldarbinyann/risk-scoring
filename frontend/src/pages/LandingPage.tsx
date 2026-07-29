import { useState } from "react";
import { useNavigate } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { HeroInput } from "@/components/hero/HeroInput";
import { ChainPicker } from "@/components/hero/ChainPicker";
import { TypewriterText } from "@/components/hero/TypewriterText";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { typewriterDurationMs } from "@/hooks/useTypewriter";
import { createScan, getChainCandidates } from "@/lib/api";
import { isEvmAddress } from "@/lib/address";
import { useI18n } from "@/lib/i18n/context";
import type { ChainCandidate } from "@/lib/types";

interface PickerData {
  address: string;
  chains: ChainCandidate[];
}

type LandingStep =
  | { kind: "input" }
  | { kind: "selecting"; picker: PickerData }
  | { kind: "leaving"; groupId: string; picker: PickerData | null };

const CROSSFADE = { duration: 0.35, ease: "easeInOut" } as const;
const FADED = { opacity: 0, scale: 0.98 };
const SHOWN = { opacity: 1, scale: 1 };

export function LandingPage() {
  const navigate = useNavigate();
  const { t } = useI18n();
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);
  const [busyChainId, setBusyChainId] = useState<number | null>(null);
  const [step, setStep] = useState<LandingStep>({ kind: "input" });
  const isLeaving = step.kind === "leaving";

  const picker = step.kind === "selecting" ? step.picker : step.kind === "leaving" ? step.picker : null;

  async function startScan(scanAddress: string, chainId: number, picker: PickerData | null) {
    setError(null);
    setBusyChainId(chainId);
    try {
      const { groupId } = await createScan({ address: scanAddress, chainIds: [chainId] });
      setStep({ kind: "leaving", groupId, picker });
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorCreateFailed"));
      setBusyChainId(null);
      setIsDetecting(false);
    }
  }

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
      const { address: normalized, chains } = await getChainCandidates(trimmedAddress);

      if (chains.length === 1) {
        await startScan(normalized, chains[0].chainId, null);
        return;
      }

      if (chains.length === 0) {
        setError(t("landing.errorChainsFailed"));
        setIsDetecting(false);
        return;
      }

      setStep({ kind: "selecting", picker: { address: normalized, chains } });
      setIsDetecting(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("landing.errorChainsFailed"));
      setIsDetecting(false);
    }
  }

  function handleChangeAddress() {
    setStep({ kind: "input" });
    setError(null);
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-8 px-6">
      <motion.div
        className="relative flex w-full max-w-4xl flex-col items-center gap-3 text-center"
        animate={isLeaving ? FADED : SHOWN}
        transition={CROSSFADE}
        onAnimationComplete={() => {
          if (step.kind === "leaving") navigate(`/scan/${step.groupId}`);
        }}
      >
        <AnimatePresence mode="wait">
          {picker ? (
            <motion.div
              key="selecting"
              initial={FADED}
              animate={SHOWN}
              exit={FADED}
              transition={CROSSFADE}
              className="flex w-full flex-col items-center gap-3"
            >
              <TypewriterText
                as="h1"
                text={t("landing.chainPickerTitle")}
                className="min-h-9 text-balance font-sans text-3xl font-semibold text-text sm:min-h-14 sm:text-4xl"
              />
              <ChainPicker
                address={picker.address}
                chains={picker.chains}
                busyChainId={busyChainId}
                onSelect={(chainId) => startScan(picker.address, chainId, picker)}
                onChangeAddress={handleChangeAddress}
              />
            </motion.div>
          ) : (
            <motion.div
              key="input"
              initial={FADED}
              animate={SHOWN}
              exit={FADED}
              transition={CROSSFADE}
              className="flex w-full flex-col items-center gap-3"
            >
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
