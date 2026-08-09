import { motion } from "motion/react";
import { NavLink } from "react-router";
import type { UserView } from "@/lib/types";
import { useI18n } from "@/lib/i18n/context";
import { cn } from "@/lib/cn";
import { ACCOUNT_LINKS, type NavLinkItem } from "@/lib/navLinks";

interface MobileNavPanelProps {
  navItems: readonly NavLinkItem[];
  isHomeFlow: boolean;
  isAuthenticated: boolean;
  user: UserView | null;
  onLogout: () => Promise<void>;
  onClose: () => void;
}

const PANEL_ITEM_CLASSES =
  "block rounded-base px-3 py-2.5 font-sans text-sm text-text-dim transition-colors hover:bg-surface-2 hover:text-text";

export function MobileNavPanel({ navItems, isHomeFlow, isAuthenticated, user, onLogout, onClose }: MobileNavPanelProps) {
  const { t } = useI18n();

  return (
    <motion.div
      id="mobile-nav-panel"
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      transition={{ duration: 0.15, ease: "easeOut" }}
      className="absolute inset-x-0 top-full z-10 border-b border-border bg-surface p-3 shadow-lg lg:hidden"
    >
      <div className="flex flex-col gap-1">
        {navItems.map((item) => {
          const isHomeItem = item.to === "/" && isHomeFlow;

          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              onClick={onClose}
              className={({ isActive }) => cn(PANEL_ITEM_CLASSES, (isActive || isHomeItem) && "bg-surface-2 text-accent")}
            >
              {t(item.labelKey)}
            </NavLink>
          );
        })}
      </div>

      <div className="mt-2 flex flex-col gap-1 border-t border-border pt-2">
        {isAuthenticated && user ? (
          <>
            {ACCOUNT_LINKS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={onClose}
                className={({ isActive }) => cn(PANEL_ITEM_CLASSES, isActive && "bg-surface-2 text-accent")}
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
            <button
              type="button"
              onClick={() => {
                onClose();
                void onLogout();
              }}
              className={cn(PANEL_ITEM_CLASSES, "w-full text-left")}
            >
              {t("auth.logout")}
            </button>
          </>
        ) : (
          <>
            <NavLink to="/auth" onClick={onClose} className={PANEL_ITEM_CLASSES}>
              {t("nav.login")}
            </NavLink>
            <NavLink
              to="/register"
              onClick={onClose}
              className="block rounded-base bg-accent px-3 py-2.5 text-center font-sans text-sm font-medium text-bg transition-colors hover:bg-accent-press"
            >
              {t("nav.register")}
            </NavLink>
          </>
        )}
      </div>
    </motion.div>
  );
}
