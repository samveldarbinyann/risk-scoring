import { motion } from "motion/react";
import { useI18n } from "@/lib/i18n/context";

interface EvidenceListProps {
  explanation: string;
  decisiveSignals: string[];
  manualChecks: string[];
}

const containerVariants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 6 },
  show: { opacity: 1, y: 0 },
};

export function EvidenceList({ explanation, decisiveSignals, manualChecks }: EvidenceListProps) {
  const { t } = useI18n();

  return (
    <motion.div variants={containerVariants} initial="hidden" animate="show" className="flex flex-col gap-6 p-6">
      <div>
        <h3 className="mb-3 font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.explanation")}</h3>
        <p className="text-sm leading-relaxed text-text">{explanation}</p>
      </div>

      {decisiveSignals.length > 0 && (
        <div className="border-t border-border pt-6">
          <h3 className="mb-3 font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.decisiveSignals")}</h3>
          <ul className="flex flex-col gap-2">
            {decisiveSignals.map((signal, index) => (
              <motion.li key={index} variants={itemVariants} className="font-mono text-sm text-text">
                {signal}
              </motion.li>
            ))}
          </ul>
        </div>
      )}

      {manualChecks.length > 0 && (
        <div className="border-t border-border pt-6">
          <h3 className="mb-3 font-sans text-xs uppercase tracking-wider text-text-dim">{t("report.manualChecks")}</h3>
          <ul className="flex flex-col gap-2">
            {manualChecks.map((check, index) => (
              <motion.li key={index} variants={itemVariants} className="text-sm text-text-dim">
                {check}
              </motion.li>
            ))}
          </ul>
        </div>
      )}
    </motion.div>
  );
}
