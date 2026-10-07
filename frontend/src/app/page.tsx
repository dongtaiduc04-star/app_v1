"use client";

import Link from "next/link";
import { SiteHeader } from "@/components/site-header";
import { useLanguage } from "@/components/language-provider";

export default function Home() {
  const { dictionary: t } = useLanguage();
  return (
    <div className="landing-page">
      <SiteHeader />
      <main>
        <section className="hero">
          <div className="hero-copy">
            <p className="hero-badge"><i />{t.landing.badge}</p>
            <h1>{t.landing.titleA}<br /><em>{t.landing.titleB}</em></h1>
            <p className="hero-description">{t.landing.description}</p>
            <div className="hero-actions">
              <Link className="button" href="/register">{t.landing.primary}<span>↗</span></Link>
            </div>
          </div>
        </section>
      </main>
      <footer><span className="wordmark">getlink<span>_dtd</span></span><small>© {new Date().getFullYear()}</small></footer>
    </div>
  );
}
