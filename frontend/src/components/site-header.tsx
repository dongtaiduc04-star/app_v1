"use client";

import Link from "next/link";
import { useLanguage } from "./language-provider";

export function SiteHeader({ dashboard = false }: { dashboard?: boolean }) {
  const { locale, setLocale, dictionary: t } = useLanguage();

  return (
    <header className="site-header">
      <Link className="wordmark" href="/" aria-label="getlink_dtd">
        getlink<span>_dtd</span>
      </Link>
      <nav aria-label="Primary navigation">
        <button
          className="language-switch"
          type="button"
          onClick={() => setLocale(locale === "vi" ? "en" : "vi")}
          aria-label={t.common.language}
        >
          {locale === "vi" ? t.common.en : t.common.vi}
        </button>
        {dashboard ? (
          <Link className="nav-link" href="/dashboard">{t.nav.dashboard}</Link>
        ) : (
          <>
            <Link className="nav-link desktop-link" href="/login">{t.nav.login}</Link>
            <Link className="button button-small" href="/register">{t.nav.register}</Link>
          </>
        )}
      </nav>
    </header>
  );
}
