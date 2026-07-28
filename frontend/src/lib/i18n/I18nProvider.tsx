import { useEffect, useMemo, useState, type ReactNode } from "react";
import { getMessages, setApiLocale } from "@/lib/api";
import type { Locale } from "@/lib/i18n/messageKeys";
import { I18nContext, type I18nContextValue } from "@/lib/i18n/context";

const STORAGE_KEY = "risk-scoring:locale";

function detectLocale(): Locale {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "en" || stored === "ru") return stored;
  return navigator.language.toLowerCase().startsWith("ru") ? "ru" : "en";
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(detectLocale);
  const [messages, setMessages] = useState<Record<string, string> | null>(null);

  useEffect(() => {
    let cancelled = false;
    setApiLocale(locale);
    localStorage.setItem(STORAGE_KEY, locale);
    document.documentElement.lang = locale;

    getMessages()
      .then((loaded) => {
        if (!cancelled) setMessages(loaded);
      })
      .catch(() => {
        if (!cancelled) setMessages({});
      });

    return () => {
      cancelled = true;
    };
  }, [locale]);

  const value = useMemo<I18nContextValue>(
    () => ({
      locale,
      setLocale: setLocaleState,
      t: (key) => messages?.[key] ?? key,
    }),
    [locale, messages],
  );

  if (!messages) return null;

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}
