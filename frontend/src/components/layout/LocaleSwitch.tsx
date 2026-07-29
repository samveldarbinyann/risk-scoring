import { motion } from "motion/react";
import { useI18n } from "@/lib/i18n/context";
import { LOCALES } from "@/lib/i18n/messageKeys";
import { cn } from "@/lib/cn";

interface LocaleSwitchProps {
  className?: string;
  onSelect?: () => void;
}

export function LocaleSwitch({ className, onSelect }: LocaleSwitchProps) {
  const { locale, setLocale } = useI18n();

  return (
    <div className={cn("flex items-center gap-1 rounded-base border border-border p-1 font-mono text-xs", className)}>
      {LOCALES.map((option) => (
        <button
          key={option}
          type="button"
          onClick={() => {
            setLocale(option);
            onSelect?.();
          }}
          className={cn(
            "relative rounded-base px-2 py-1 uppercase transition-colors",
            option === locale ? "text-bg" : "text-text-dim hover:text-text",
          )}
        >
          {option === locale && (
            <motion.span
              layoutId="locale-switch-highlight"
              className="absolute inset-0 rounded-base bg-accent"
              transition={{ type: "spring", stiffness: 500, damping: 35 }}
            />
          )}
          <span className="relative">{option}</span>
        </button>
      ))}
    </div>
  );
}
