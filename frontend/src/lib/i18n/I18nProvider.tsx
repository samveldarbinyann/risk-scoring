import { useEffect, useMemo, useState, type ReactNode } from "react";
import { getMessages, setApiLocale } from "@/lib/api";
import { LOCALES, type Locale } from "@/lib/i18n/messageKeys";
import { I18nContext, type I18nContextValue } from "@/lib/i18n/context";

const STORAGE_KEY = "risk-scoring:locale";

type MessageBundles = Record<Locale, Record<string, string>>;

function detectLocale(): Locale {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "en" || stored === "ru") return stored;
  return navigator.language.toLowerCase().startsWith("ru") ? "ru" : "en";
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(detectLocale);
  const [bundles, setBundles] = useState<MessageBundles | null>(null);

  useEffect(() => {
    let cancelled = false;

    const loaded = LOCALES.map(
      async (loc) => [loc, await getMessages(loc).catch((): Record<string, string> => ({}))] as const,
    );

    Promise.all(loaded).then((entries) => {
      if (cancelled) return;
      setBundles(Object.fromEntries(entries) as MessageBundles);
    });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    setApiLocale(locale);
    localStorage.setItem(STORAGE_KEY, locale);
    document.documentElement.lang = locale;
  }, [locale]);

  const value = useMemo<I18nContextValue>(
    () => ({
      locale,
      setLocale: setLocaleState,
      t: (key) => bundles?.[locale]?.[key] ?? key,
    }),
    [locale, bundles],
  );

  if (!bundles) return null;

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}
