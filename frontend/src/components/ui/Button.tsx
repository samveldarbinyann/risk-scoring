import type { ButtonHTMLAttributes } from "react";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/lib/cn";

type ButtonVariant = "primary" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  isLoading?: boolean;
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: "bg-accent text-bg hover:bg-accent-press disabled:bg-surface-2 disabled:text-text-faint",
  ghost: "border border-border bg-transparent text-text hover:border-accent disabled:text-text-faint",
};

export function Button({ variant = "primary", isLoading, disabled, className, children, ...props }: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      className={cn(
        "flex h-12 items-center justify-center gap-2 rounded-base px-6 font-sans text-base font-medium",
        "transition-colors disabled:cursor-not-allowed",
        VARIANT_CLASSES[variant],
        className,
      )}
      {...props}
    >
      {isLoading ? <Spinner /> : children}
    </button>
  );
}
