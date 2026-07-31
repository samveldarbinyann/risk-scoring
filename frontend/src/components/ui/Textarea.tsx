import type { TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement>;

export function Textarea({ className, ...props }: TextareaProps) {
  return (
    <textarea
      className={cn(
        "w-full resize-y rounded-panel border border-border bg-surface px-4 py-3 font-mono text-sm leading-relaxed text-text outline-none placeholder:text-text-faint",
        "transition-colors focus:border-accent disabled:cursor-not-allowed disabled:text-text-faint",
        className,
      )}
      {...props}
    />
  );
}
