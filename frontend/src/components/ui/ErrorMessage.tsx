import { AnimatePresence, motion } from "motion/react";
import { useTypewriter } from "@/hooks/useTypewriter";
import { cn } from "@/lib/cn";

type ErrorMessageSize = "xs" | "sm";

interface ErrorMessageProps {
  message: string | null;
  size?: ErrorMessageSize;
}

const SIZE_CLASSES: Record<ErrorMessageSize, string> = {
  xs: "text-xs",
  sm: "text-sm",
};

export function ErrorMessage({ message, size = "xs" }: ErrorMessageProps) {
  const { text: displayed } = useTypewriter(message ?? "");

  return (
    <AnimatePresence>
      {message && (
        <motion.p
          key={message}
          initial={{ opacity: 0, y: -4 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -4 }}
          transition={{ duration: 0.2, ease: "easeOut" }}
          className={cn("font-mono text-risk-critical", SIZE_CLASSES[size])}
        >
          {displayed}
          <motion.span
            aria-hidden
            className="ml-0.5 inline-block h-[0.9em] w-[0.5em] translate-y-[0.1em] bg-risk-critical align-baseline"
            animate={{ opacity: [1, 1, 0, 0] }}
            transition={{ duration: 0.9, repeat: Infinity, ease: "linear", times: [0, 0.5, 0.5, 1] }}
          />
        </motion.p>
      )}
    </AnimatePresence>
  );
}
