import { cn } from "@/lib/cn";

interface CodeBlockProps {
  code: string;
  className?: string;
}

export function CodeBlock({ code, className }: CodeBlockProps) {
  return (
    <pre
      className={cn(
        "overflow-x-auto rounded-panel border border-border bg-bg p-4 font-mono text-xs leading-relaxed text-text-dim",
        className,
      )}
    >
      <code>{code}</code>
    </pre>
  );
}
