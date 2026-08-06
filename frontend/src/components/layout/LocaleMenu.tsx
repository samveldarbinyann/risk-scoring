import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { LocaleSwitch } from "@/components/layout/LocaleSwitch";
import { useDismissableMenu } from "@/hooks/useDismissableMenu";
import { useI18n } from "@/lib/i18n/context";
import { cn } from "@/lib/cn";

interface LocaleMenuProps {
  className?: string;
}

export function LocaleMenu({ className }: LocaleMenuProps) {
  const { t } = useI18n();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useDismissableMenu<HTMLDivElement>(isOpen, () => setIsOpen(false));

  return (
    <div className={className}>
      <div ref={containerRef} className="relative">
        <button
          type="button"
          onClick={() => setIsOpen((open) => !open)}
          aria-label={t("nav.language")}
          aria-expanded={isOpen}
          className={cn("flex items-center justify-center text-text-dim transition-colors hover:text-text", isOpen && "text-accent")}
        >
          <GlobeIcon className="h-5 w-5" />
        </button>
        <AnimatePresence>
          {isOpen && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: -4 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: -4 }}
              transition={{ duration: 0.15, ease: "easeOut" }}
              className="absolute left-1/2 top-full z-10 mt-2 origin-top -translate-x-1/2"
            >
              <LocaleSwitch onSelect={() => setIsOpen(false)} className="bg-surface" />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

function GlobeIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className={className}>
      <circle cx="12" cy="12" r="9" />
      <path d="M3 12h18" />
      <path d="M12 3c2.5 2.5 3.75 5.5 3.75 9s-1.25 6.5-3.75 9c-2.5-2.5-3.75-5.5-3.75-9S9.5 5.5 12 3Z" />
    </svg>
  );
}
