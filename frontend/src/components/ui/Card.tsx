import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

interface CardProps extends HTMLAttributes<HTMLElement> {
  title?: string;
  children: ReactNode;
}

export function Card({ title, className, children, ...props }: CardProps) {
  return (
    <section className={cn("rounded-panel border border-border bg-surface p-6", className)} {...props}>
      {title && <h3 className="mb-4 font-sans text-xs uppercase tracking-wider text-text-dim">{title}</h3>}
      {children}
    </section>
  );
}
