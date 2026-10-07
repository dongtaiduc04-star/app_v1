"use client";

import { createContext, useContext, useEffect, useMemo, useSyncExternalStore } from "react";
import { dictionaries, type Dictionary, type Locale } from "@/lib/i18n";

type LanguageContextValue = {
  locale: Locale;
  dictionary: Dictionary;
  setLocale: (locale: Locale) => void;
};

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const locale = useSyncExternalStore(
    (onChange) => {
      window.addEventListener("storage", onChange);
      window.addEventListener("getlink-language", onChange);
      return () => {
        window.removeEventListener("storage", onChange);
        window.removeEventListener("getlink-language", onChange);
      };
    },
    () => {
      const saved = window.localStorage.getItem("getlink_locale");
      if (saved === "en" || saved === "vi") return saved;
      return navigator.language.toLowerCase().startsWith("en") ? "en" : "vi";
    },
    () => "vi",
  ) as Locale;

  function setLocale(next: Locale) {
    window.localStorage.setItem("getlink_locale", next);
    window.dispatchEvent(new Event("getlink-language"));
  }

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const value = useMemo(
    () => ({ locale, dictionary: dictionaries[locale] as Dictionary, setLocale }),
    [locale],
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const value = useContext(LanguageContext);
  if (!value) throw new Error("useLanguage must be used inside LanguageProvider");
  return value;
}
