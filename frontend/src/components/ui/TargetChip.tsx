import { useEffect, useState } from "react";
import { formatAddress } from "@/lib/format";
import { cn } from "@/lib/cn";

interface TargetChipProps {
  value: string;
  className?: string;
}

export function TargetChip({ value, className }: TargetChipProps) {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(false), 1500);
    return () => window.clearTimeout(timer);
  }, [copied]);

  async function handleClick() {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
    } catch {
      // clipboard unavailable — nothing else to fall back to
    }
  }

  return (
    <button
      type="button"
      onClick={() => void handleClick()}
      title={value}
      className={cn(
        "inline-flex cursor-pointer items-center gap-1.5 font-mono text-accent transition-colors hover:text-accent-press",
        className,
      )}
    >
      <span className="truncate">{formatAddress(value)}</span>
      {copied && (
        <svg
          viewBox="0 0 20 20"
          aria-hidden="true"
          className="h-3.5 w-3.5 shrink-0"
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M5 10.5l3 3 7-7" />
        </svg>
      )}
    </button>
  );
}
