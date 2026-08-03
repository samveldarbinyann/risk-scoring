import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { NavLink } from "react-router";
import type { UserView } from "@/lib/types";
import { useI18n } from "@/lib/i18n/context";
import { cn } from "@/lib/cn";

interface AccountMenuProps {
  user: UserView;
  onLogout: () => Promise<void>;
}

const ACCOUNT_LINKS = [
  { to: "/dashboard", labelKey: "nav.dashboard" },
  { to: "/watchlist", labelKey: "nav.watchlist" },
  { to: "/alerts", labelKey: "nav.alerts" },
  { to: "/settings", labelKey: "nav.settings" },
] as const;

export function AccountMenu({ user, onLogout }: AccountMenuProps) {
  const { t } = useI18n();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const initials = `${user.firstName[0] ?? ""}${user.lastName[0] ?? ""}`.toUpperCase() || user.username.slice(0, 2).toUpperCase();

  useEffect(() => {
    if (!isOpen) return;

    function closeMenu(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }

    document.addEventListener("mousedown", closeMenu);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeMenu);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [isOpen]);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        aria-haspopup="menu"
        className={cn(
          "flex items-center gap-3 rounded-base border border-border bg-surface px-3 py-2 font-sans text-sm font-medium text-text-dim transition-colors hover:border-accent hover:text-text",
          isOpen && "border-accent text-text",
        )}
      >
        <span className="flex h-7 w-7 items-center justify-center rounded-base bg-surface-2 font-mono text-xs text-accent">{initials}</span>
        <span>{user.username}</span>
        <ChevronIcon className={cn("h-4 w-4 transition-transform", isOpen && "rotate-180")} />
      </button>
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -4 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -4 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="absolute left-1/2 top-full z-10 mt-2 w-52 origin-top -translate-x-1/2 rounded-panel border border-border bg-surface p-2 shadow-lg"
          >
            <div className="flex flex-col gap-1 border-b border-border pb-2">
              {ACCOUNT_LINKS.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setIsOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      "block rounded-base px-3 py-2 font-sans text-sm text-text-dim transition-colors hover:bg-surface-2 hover:text-text",
                      isActive && "bg-surface-2 text-accent",
                    )
                  }
                >
                  {t(item.labelKey)}
                </NavLink>
              ))}
            </div>
            <button
              type="button"
              onClick={() => {
                setIsOpen(false);
                void onLogout();
              }}
              className="mt-2 w-full rounded-base px-3 py-2 text-left font-sans text-sm text-text-dim transition-colors hover:bg-surface-2 hover:text-text"
            >
              {t("auth.logout")}
            </button>
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
