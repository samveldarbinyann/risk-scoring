import { createElement } from "react";
import { motion } from "motion/react";
import { useTypewriter } from "@/hooks/useTypewriter";

interface TypewriterTextProps {
  text: string;
  delayMs?: number;
  as?: "h1" | "p";
  className?: string;
}

export function TypewriterText({ text, delayMs = 0, as = "p", className }: TypewriterTextProps) {
  const { text: displayed, isTyping } = useTypewriter(text, delayMs);

  return createElement(
    as,
    { className },
    <>
      {displayed}
      {isTyping && (
        <motion.span
          aria-hidden
          className="ml-0.5 inline-block h-[0.85em] w-[0.5em] translate-y-[0.12em] bg-current align-baseline"
          animate={{ opacity: [1, 1, 0, 0] }}
          transition={{ duration: 0.9, repeat: Infinity, ease: "linear", times: [0, 0.5, 0.5, 1] }}
        />
      )}
    </>,
  );
}
