import { AnimatePresence, motion } from "motion/react";
import { TypewriterCaret } from "@/components/ui/TypewriterCaret";
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
          <TypewriterCaret />
        </motion.p>
      )}
    </AnimatePresence>
  );
}
