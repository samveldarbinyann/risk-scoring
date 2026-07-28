import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type ButtonVariant = "primary" | "secondary" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary:
    "bg-accent text-bg hover:bg-accent-press active:bg-accent-press disabled:bg-surface-2 disabled:text-text-faint",
  secondary:
    "border border-border bg-surface text-text hover:border-accent active:bg-surface-2 disabled:text-text-faint disabled:hover:border-border",
  ghost:
    "text-text-dim hover:text-text active:text-text disabled:text-text-faint",
};

export function Button({ variant = "primary", className, disabled, ...props }: ButtonProps) {
  return (
    <button
      disabled={disabled}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-base px-4 py-2.5 font-sans text-sm font-medium",
        "transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg",
        "disabled:cursor-not-allowed",
        VARIANT_CLASSES[variant],
        className,
      )}
      {...props}
    />
  );
}
