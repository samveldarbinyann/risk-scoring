import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { useDismissableMenu } from "@/hooks/useDismissableMenu";
import { cn } from "@/lib/cn";

interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectProps {
  options: SelectOption[];
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  className?: string;
  "aria-label"?: string;
}

export function Select({ options, value, onChange, disabled, className, "aria-label": ariaLabel }: SelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useDismissableMenu<HTMLDivElement>(isOpen, () => setIsOpen(false));

  const selected = options.find((option) => option.value === value);

  function handleSelect(option: SelectOption) {
    if (option.disabled) return;
    onChange(option.value);
    setIsOpen(false);
  }

  return (
    <div ref={containerRef} className={cn("relative", className)}>
      <button
        type="button"
        disabled={disabled}
        onClick={() => setIsOpen((open) => !open)}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        className={cn(
          "flex h-full w-full items-center rounded-base border border-border bg-surface px-4 py-2.5 pr-10 font-mono text-sm text-text outline-none",
          "transition-colors focus:border-accent disabled:cursor-not-allowed disabled:text-text-faint",
          isOpen && "border-accent",
        )}
      >
        <span className="truncate">{selected?.label}</span>
      </button>
      <ChevronIcon
        className={cn(
          "pointer-events-none absolute right-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-text-dim transition-transform",
          isOpen && "rotate-180",
        )}
      />

      <AnimatePresence>
        {isOpen && (
          <motion.div
            role="listbox"
            initial={{ opacity: 0, scale: 0.95, y: -4 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -4 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="absolute left-0 top-full z-10 mt-2 min-w-full w-max origin-top rounded-panel border border-border bg-surface p-2 shadow-lg"
          >
            {options.map((option) => (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={option.value === value}
                disabled={option.disabled}
                onClick={() => handleSelect(option)}
                className={cn(
                  "block w-full whitespace-nowrap rounded-base px-3 py-2 text-left font-mono text-sm transition-colors",
                  option.disabled
                    ? "cursor-not-allowed text-text-faint"
                    : option.value === value
                      ? "bg-surface-2 text-accent"
                      : "text-text-dim hover:bg-surface-2 hover:text-text",
                )}
              >
                {option.label}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function ChevronIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className={className}>
      <path d="m6 9 6 6 6-6" />
    </svg>
  );
}
