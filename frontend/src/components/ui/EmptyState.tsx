import { LinkButton } from "@/components/ui/LinkButton";
import { TypewriterCaret } from "@/components/ui/TypewriterCaret";

interface EmptyStateProps {
  message: string;
  hint?: string;
  ctaLabel?: string;
  ctaTo?: string;
}

export function EmptyState({ message, hint, ctaLabel, ctaTo }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-start gap-3">
      <p className="font-mono text-sm text-text-faint">
        &gt; {message}
        <TypewriterCaret />
      </p>
      {hint && <p className="max-w-md text-sm leading-relaxed text-text-dim">{hint}</p>}
      {ctaLabel && ctaTo && (
        <LinkButton to={ctaTo} variant="ghost">
          {ctaLabel}
        </LinkButton>
      )}
    </div>
  );
}
