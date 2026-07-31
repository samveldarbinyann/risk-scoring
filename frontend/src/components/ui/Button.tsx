import type { ButtonHTMLAttributes } from "react";
import { BUTTON_BASE_CLASSES, BUTTON_VARIANT_CLASSES, type ButtonVariant } from "@/components/ui/buttonStyles";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/lib/cn";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  isLoading?: boolean;
}

export function Button({ variant = "primary", isLoading, disabled, className, children, ...props }: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled || isLoading}
      aria-busy={isLoading}
      className={cn(BUTTON_BASE_CLASSES, "disabled:cursor-not-allowed", BUTTON_VARIANT_CLASSES[variant], className)}
      {...props}
    >
      {isLoading ? <Spinner /> : children}
    </button>
  );
}
