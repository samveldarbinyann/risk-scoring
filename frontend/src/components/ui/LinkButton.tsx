import type { ReactNode } from "react";
import { NavLink } from "react-router";
import { BUTTON_BASE_CLASSES, BUTTON_VARIANT_CLASSES, type ButtonVariant } from "@/components/ui/buttonStyles";
import { cn } from "@/lib/cn";

interface LinkButtonProps {
  to: string;
  variant?: ButtonVariant;
  className?: string;
  children: ReactNode;
}

export function LinkButton({ to, variant = "primary", className, children }: LinkButtonProps) {
  return (
    <NavLink to={to} className={cn(BUTTON_BASE_CLASSES, BUTTON_VARIANT_CLASSES[variant], className)}>
      {children}
    </NavLink>
  );
}
