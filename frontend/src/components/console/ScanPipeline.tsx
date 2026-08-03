import { motion } from "motion/react";
import type { ScanProgressMessage, ScanStage } from "@/lib/types";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import { STAGE_CODENAME } from "@/components/console/StageCopy";

const PIPELINE_STAGES: ScanStage[] = ["PENDING", "FETCHING", "ENRICHING", "ANALYZING", "COMPLETED"];

type NodeVariant = "current" | "complete" | "critical" | "pending";

const NODE_VARIANT: Record<NodeVariant, { border: string; text: string }> = {
  current: { border: "border-accent", text: "text-accent" },
  complete: { border: "border-risk-low", text: "text-risk-low" },
  critical: { border: "border-risk-critical", text: "text-risk-critical" },
  pending: { border: "border-border", text: "text-text-faint" },
};

interface ScanPipelineProps {
  lines: ScanProgressMessage[];
}

export function ScanPipeline({ lines }: ScanPipelineProps) {
  const { t } = useI18n();
  const failed = lines.some((line) => line.stage === "FAILED");
  const highestStageIndex = Math.max(
    0,
    ...lines.filter((line) => line.stage !== "FAILED").map((line) => PIPELINE_STAGES.indexOf(line.stage)),
  );

  return (
    <section className="p-4">
      <div className="mb-4 flex items-center gap-3">
        <span className="h-2 w-2 rounded-base bg-accent" />
        <h2 className="font-mono text-xs uppercase tracking-widest text-text-dim">{t("console.pipeline")}</h2>
      </div>
      <div className="flex items-start">
        {PIPELINE_STAGES.map((stage, index) => {
          const isLastStage = index === PIPELINE_STAGES.length - 1;
          const variant: NodeVariant = failed
            ? index < highestStageIndex
              ? "complete"
              : index === highestStageIndex
                ? "critical"
                : "pending"
            : index < highestStageIndex
              ? "complete"
              : index === highestStageIndex
                ? isLastStage
                  ? "complete"
                  : "current"
                : "pending";

          const connectorState = index < highestStageIndex ? "passed" : failed ? "broken" : "pending";

          return (
            <div key={stage} className="contents">
              <StageNode index={index} stage={stage} variant={variant} />
              {index < PIPELINE_STAGES.length - 1 && <StageConnector state={connectorState} />}
            </div>
          );
        })}
      </div>
    </section>
  );
}

interface StageNodeProps {
  index: number;
  stage: ScanStage;
  variant: NodeVariant;
}

function StageNode({ index, stage, variant }: StageNodeProps) {
  const { t } = useI18n();
  const { border, text } = NODE_VARIANT[variant];
  const isCurrent = variant === "current";

  return (
    <div className="flex w-10 flex-none flex-col items-center gap-2 md:w-20">
      <motion.div
        animate={isCurrent ? { opacity: [0.7, 1, 0.7] } : { opacity: 1 }}
        transition={isCurrent ? { duration: 1.4, ease: "easeInOut", repeat: Infinity } : undefined}
        className={cn("relative flex h-8 w-8 items-center justify-center font-mono text-xs", text)}
      >
        <span aria-hidden className={cn("absolute left-0 top-0 h-2.5 w-2.5 border-l-2 border-t-2", border)} />
        <span aria-hidden className={cn("absolute right-0 top-0 h-2.5 w-2.5 border-r-2 border-t-2", border)} />
        <span aria-hidden className={cn("absolute bottom-0 left-0 h-2.5 w-2.5 border-b-2 border-l-2", border)} />
        <span aria-hidden className={cn("absolute bottom-0 right-0 h-2.5 w-2.5 border-b-2 border-r-2", border)} />
        <span>{String(index + 1).padStart(2, "0")}</span>
      </motion.div>
      <span className={cn("hidden w-full text-center font-mono text-xs md:block", text)}>{t(STAGE_CODENAME[stage])}</span>
    </div>
  );
}

interface StageConnectorProps {
  state: "passed" | "broken" | "pending";
}

function StageConnector({ state }: StageConnectorProps) {
  if (state === "broken") {
    return <div className="mt-4 h-0 flex-1 border-t-2 border-dashed border-risk-critical" />;
  }

  return (
    <div className="relative mt-4 h-px flex-1 bg-border">
      {state === "passed" && (
        <motion.div
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 0.35, ease: "easeOut" }}
          className="absolute inset-0 origin-left bg-accent"
        />
      )}
    </div>
  );
}
