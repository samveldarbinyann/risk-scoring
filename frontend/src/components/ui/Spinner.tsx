import { motion } from "motion/react";
import { cn } from "@/lib/cn";

interface SpinnerProps {
  className?: string;
}

export function Spinner({ className }: SpinnerProps) {
  return (
    <motion.span
      className={cn("inline-block h-4 w-4 rounded-full border-2 border-border border-t-accent", className)}
      animate={{ rotate: 360 }}
      transition={{ duration: 0.8, repeat: Infinity, ease: "linear" }}
    />
  );
}
