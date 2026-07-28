import { NavLink } from "react-router";
import { cn } from "@/lib/cn";
import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";
import { LocaleSwitch } from "@/components/layout/LocaleSwitch";

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

const AUTH_LINK_CLASSES = "rounded-base px-3 py-1.5 font-sans text-sm font-medium transition-colors";

export function NavBar() {
  const { t } = useI18n();

  return (
    <nav className="flex items-center gap-8 border-b border-border bg-surface px-6 py-4">
      <span className="font-mono text-sm font-medium text-accent">RISK//SCAN</span>
      <ul className="flex flex-1 items-center gap-5">
        {NAV_ITEMS.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) =>
                cn("font-sans text-sm text-text-dim transition-colors hover:text-text", isActive && "text-accent")
              }
            >
              {t(item.labelKey)}
            </NavLink>
          </li>
        ))}
      </ul>
      <LocaleSwitch />
      <div className="flex items-center gap-2">
        <NavLink to="/auth" className={cn(AUTH_LINK_CLASSES, "text-text-dim hover:text-text")}>
          {t("nav.login")}
        </NavLink>
        <NavLink to="/register" className={cn(AUTH_LINK_CLASSES, "bg-accent text-bg hover:bg-accent-press")}>
          {t("nav.register")}
        </NavLink>
      </div>
    </nav>
  );
}
