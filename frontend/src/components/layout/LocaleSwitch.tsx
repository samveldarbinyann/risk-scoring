import { useI18n } from "@/lib/i18n/context";
import { LOCALES } from "@/lib/i18n/messageKeys";
import { cn } from "@/lib/cn";

interface LocaleSwitchProps {
  className?: string;
}

export function LocaleSwitch({ className }: LocaleSwitchProps) {
  const { locale, setLocale } = useI18n();

  return (
    <div className={cn("flex items-center gap-1 rounded-base border border-border p-1 font-mono text-xs", className)}>
      {LOCALES.map((option) => (
        <button
          key={option}
          type="button"
          onClick={() => setLocale(option)}
          className={cn(
            "rounded-base px-2 py-1 uppercase transition-colors",
            option === locale ? "bg-accent text-bg" : "text-text-dim hover:text-text",
          )}
        >
          {option}
        </button>
      ))}
    </div>
  );
}
