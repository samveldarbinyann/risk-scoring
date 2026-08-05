import type { ReactNode } from "react";

interface DocsSectionProps {
  title: string;
  body: string;
  children?: ReactNode;
}

export function DocsSection({ title, body, children }: DocsSectionProps) {
  return (
    <section className="flex flex-col gap-4 border-t border-border pt-8">
      <header className="flex flex-col gap-2">
        <h2 className="font-sans text-2xl font-semibold text-text">{title}</h2>
        <p className="max-w-2xl text-sm leading-relaxed text-text-dim">{body}</p>
      </header>
      {children}
    </section>
  );
}
