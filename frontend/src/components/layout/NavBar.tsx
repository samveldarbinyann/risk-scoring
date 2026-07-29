import { motion } from "motion/react";
import { NavLink } from "react-router";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";

const LAYOUT_SPRING = { type: "spring", stiffness: 500, damping: 40 } as const;

interface NavItem {
  to: string;
  labelKey: MessageKey;
}

// Для незалогиненных пользователей доступен только скан + маркетинговые страницы.
// Дашборд/watchlist/алерты/настройки появятся в хидере после реализации auth.
const NAV_ITEMS: NavItem[] = [
  { to: "/", labelKey: "nav.home" },
  { to: "/pricing", labelKey: "nav.pricing" },
  { to: "/docs", labelKey: "nav.docs" },
  { to: "/contact", labelKey: "nav.contact" },
];

const AUTH_LINK_CLASSES = "rounded-base px-5 py-2.5 font-sans text-base font-medium transition-colors";

export function NavBar() {
  const { t } = useI18n();

  return (
    <nav className="relative flex items-center gap-8 border-b border-border bg-surface px-6 py-4">
      <div className="flex items-start gap-2.5">
        <img src="/logo.svg" alt="AddressLens" className="h-9 w-auto" />
        <div className="flex h-9 flex-col justify-between">
          <span className="flex justify-between font-mono text-lg font-bold leading-none text-accent">
            {"ADDRESSLENS".split("").map((letter, i) => (
              <span key={i}>{letter}</span>
            ))}
          </span>
          <span className="whitespace-nowrap font-mono text-xs leading-none text-text-dim">on-chain risk, decoded</span>
        </div>
      </div>
      <ul className="flex flex-1 items-center gap-5">
        {NAV_ITEMS.map((item) => (
          <motion.li key={item.to} layout="position" transition={LAYOUT_SPRING}>
            <NavLink
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) =>
                cn(
                  "relative inline-block font-sans text-base text-text-dim transition-colors hover:text-text",
                  isActive && "text-accent",
                )
              }
            >
              {({ isActive }) => (
                <>
                  <motion.span layout transition={LAYOUT_SPRING} className="inline-block whitespace-nowrap">
                    {t(item.labelKey)}
                  </motion.span>
                  {isActive && (
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
              )}
            </NavLink>
          </motion.li>
        ))}
      </ul>
      <div className="flex items-center gap-2">
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
      </div>
    </nav>
  );
}
