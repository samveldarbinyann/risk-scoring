import { motion } from "motion/react";

export function TypewriterCaret() {
  return (
    <motion.span
      aria-hidden
      className="ml-0.5 inline-block h-[0.85em] w-[0.5em] translate-y-[0.12em] bg-current align-baseline"
      animate={{ opacity: [1, 1, 0, 0] }}
      transition={{ duration: 0.9, repeat: Infinity, ease: "linear", times: [0, 0.5, 0.5, 1] }}
    />
  );
}
