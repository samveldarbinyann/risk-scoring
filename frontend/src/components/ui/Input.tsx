import type { InputHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type InputProps = InputHTMLAttributes<HTMLInputElement>;

export function Input({ className, ...props }: InputProps) {
  return (
    <input
      className={cn(
        "w-full rounded-base border border-border bg-surface px-4 py-2.5 font-mono text-sm text-text outline-none placeholder:text-text-faint",
        "transition-colors focus:border-accent disabled:cursor-not-allowed disabled:text-text-faint",
        className,
      )}
      {...props}
    />
  );
}
