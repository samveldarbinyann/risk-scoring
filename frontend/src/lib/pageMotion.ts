import type { Variants } from "motion/react";

export const GRID_VARIANTS: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};

export const SECTION_VARIANTS: Variants = {
  hidden: { opacity: 0, y: 4 },
  show: { opacity: 1, y: 0, transition: { duration: 0.18, ease: "easeOut" } },
};
