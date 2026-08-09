import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { NavLink, useLocation } from "react-router";
import { AccountMenu } from "@/components/layout/AccountMenu";
import { LocaleMenu } from "@/components/layout/LocaleMenu";
import { MobileNavPanel } from "@/components/layout/MobileNavPanel";
import { useDismissableMenu } from "@/hooks/useDismissableMenu";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import { useAuth } from "@/lib/auth/context";
import type { NavLinkItem } from "@/lib/navLinks";

const LAYOUT_SPRING = { type: "spring", stiffness: 500, damping: 40 } as const;
const SCAN_FLOW_PATH = /^\/scan\/[^/]+(?:\/report)?\/?$/;

const NAV_ITEMS: NavLinkItem[] = [
  { to: "/", labelKey: "nav.home" },
  { to: "/pricing", labelKey: "nav.pricing" },
  { to: "/docs", labelKey: "nav.docs" },
  { to: "/contact", labelKey: "nav.contact" },
];

const AUTH_LINK_CLASSES = "rounded-base px-5 py-2.5 font-sans text-base font-medium transition-colors";

export function NavBar() {
  const { t } = useI18n();
  const { status, user, logout } = useAuth();
  const { pathname } = useLocation();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useDismissableMenu<HTMLDivElement>(isMenuOpen, () => setIsMenuOpen(false));
  const isAuthenticated = status === "authenticated" && user !== null;
  const isHomeFlow = pathname === "/" || SCAN_FLOW_PATH.test(pathname);

  return (
    <nav ref={menuRef} className="relative flex items-center justify-between gap-4 border-b border-border bg-surface px-6 py-4 lg:gap-8">
      <NavLink to="/" className="flex items-start gap-2.5">
        <motion.div
          animate={{
            filter: [
              "drop-shadow(0 0 0 transparent)",
              "drop-shadow(0 0 2px color-mix(in srgb, var(--color-accent) 14%, transparent))",
              "drop-shadow(0 0 0 transparent)",
            ],
          }}
          transition={{ duration: 4.5, ease: "easeInOut", repeat: Infinity }}
          className="flex items-start gap-2.5"
        >
          <img src="/logo.svg" alt="" className="h-9 w-auto" />
          <div className="inline-flex h-9 w-fit flex-col justify-between">
            <span className="font-mono text-lg font-bold leading-none tracking-normal text-accent">ADDRESSLENS</span>
            <span className="block font-mono text-xs leading-none text-text-dim">{t("nav.tagline")}</span>
          </div>
        </motion.div>
      </NavLink>
      <ul className="hidden flex-1 items-center gap-5 lg:flex">
        {NAV_ITEMS.map((item) => {
          const isHomeItem = item.to === "/" && isHomeFlow;

          return (
            <motion.li key={item.to} layout="position" transition={LAYOUT_SPRING}>
              <NavLink
                to={item.to}
                end={item.to === "/"}
                className={({ isActive }) =>
                  cn(
                    "relative inline-block font-sans text-base text-text-dim transition-colors hover:text-text",
                    (isActive || isHomeItem) && "text-accent",
                  )
                }
              >
                {({ isActive }) => {
                  const isCurrent = isActive || isHomeItem;

                  return (
                    <>
                      <motion.span layout transition={LAYOUT_SPRING} className="inline-block whitespace-nowrap">
                        {t(item.labelKey)}
                      </motion.span>
                      {isCurrent && (
                        <motion.span
                          layoutId="nav-active-underline"
                          className="absolute inset-x-0 -bottom-1.5 h-0.5 rounded-full bg-accent shadow-[0_0_6px_var(--color-accent)]"
                          animate={{ opacity: [1, 0.35, 1] }}
                          transition={{
                            layout: LAYOUT_SPRING,
                            opacity: { duration: 1.6, repeat: Infinity, repeatType: "mirror", ease: "easeInOut" },
                          }}
                        />
                      )}
                    </>
                  );
                }}
              </NavLink>
            </motion.li>
          );
        })}
      </ul>
      <div className="flex items-center gap-2">
        <LocaleMenu className="mr-2" />
        <div className="hidden items-center gap-2 lg:flex">
          {isAuthenticated ? (
            <motion.div layout transition={LAYOUT_SPRING}>
              <AccountMenu user={user} onLogout={logout} />
            </motion.div>
          ) : (
            <>
              <motion.div layout transition={LAYOUT_SPRING}>
                <NavLink to="/auth" className={cn(AUTH_LINK_CLASSES, "block text-text-dim hover:text-text")}>
                  {t("nav.login")}
                </NavLink>
              </motion.div>
              <motion.div layout transition={LAYOUT_SPRING}>
                <NavLink to="/register" className={cn(AUTH_LINK_CLASSES, "block bg-accent text-bg hover:bg-accent-press")}>
                  {t("nav.register")}
                </NavLink>
              </motion.div>
            </>
          )}
        </div>
        <button
          type="button"
          onClick={() => setIsMenuOpen((open) => !open)}
          aria-expanded={isMenuOpen}
          aria-controls="mobile-nav-panel"
          aria-label={t("nav.menu")}
          className={cn(
            "flex h-9 w-9 items-center justify-center rounded-base border border-border text-text-dim transition-colors hover:border-accent hover:text-text lg:hidden",
            isMenuOpen && "border-accent text-text",
          )}
        >
          <MenuIcon isOpen={isMenuOpen} className="h-5 w-5" />
        </button>
      </div>
      <AnimatePresence>
        {isMenuOpen && (
          <MobileNavPanel
            navItems={NAV_ITEMS}
            isHomeFlow={isHomeFlow}
            isAuthenticated={isAuthenticated}
            user={user}
            onLogout={logout}
            onClose={() => setIsMenuOpen(false)}
          />
        )}
      </AnimatePresence>
    </nav>
  );
}

function MenuIcon({ isOpen, className }: { isOpen: boolean; className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" className={className}>
      {isOpen ? (
        <>
          <path d="M6 6l12 12" />
          <path d="M18 6 6 18" />
        </>
      ) : (
        <>
          <path d="M4 7h16" />
          <path d="M4 12h16" />
          <path d="M4 17h16" />
        </>
      )}
    </svg>
  );
}
