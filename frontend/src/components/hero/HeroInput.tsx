import { Spinner } from "@/components/ui/Spinner";

interface HeroInputProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  placeholder: string;
  disabled?: boolean;
  submitLabel: string;
  isSubmitting?: boolean;
}

export function HeroInput({
  value,
  onChange,
  onSubmit,
  placeholder,
  disabled,
  submitLabel,
  isSubmitting,
}: HeroInputProps) {
  return (
    <div
      className="flex w-full flex-1 items-center gap-3 rounded-base border border-border
                    bg-surface py-2 pl-6 pr-2 font-mono transition-[border-color,box-shadow]
                    focus-within:border-accent focus-within:shadow-[0_0_28px_-6px_color-mix(in_srgb,var(--color-accent)_18%,transparent)]"
    >
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => event.key === "Enter" && onSubmit()}
        disabled={disabled}
        placeholder={placeholder}
        className="w-full min-w-0 flex-1 bg-transparent text-lg text-text outline-none placeholder:text-text-faint disabled:text-text-faint"
      />
      <button
        type="button"
        onClick={onSubmit}
        disabled={disabled}
        aria-label={submitLabel}
        aria-busy={isSubmitting}
        className="flex h-12 w-12 shrink-0 items-center justify-center rounded-base bg-accent text-bg transition-colors hover:bg-accent-press active:bg-accent-press disabled:cursor-not-allowed disabled:bg-surface-2 disabled:text-text-faint"
      >
        {isSubmitting ? <Spinner /> : <SearchIcon className="h-5 w-5" />}
      </button>
    </div>
  );
}

function SearchIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" className={className}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  );
}
