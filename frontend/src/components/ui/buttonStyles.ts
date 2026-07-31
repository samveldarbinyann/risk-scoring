export type ButtonVariant = "primary" | "ghost";

export const BUTTON_BASE_CLASSES =
  "flex h-12 items-center justify-center gap-2 rounded-base px-6 font-sans text-base font-medium transition-colors";

export const BUTTON_VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "bg-accent text-bg hover:bg-accent-press disabled:bg-surface-2 disabled:text-text-faint",
  ghost: "border border-border bg-transparent text-text hover:border-accent disabled:text-text-faint",
};
