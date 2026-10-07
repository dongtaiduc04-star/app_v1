"use client";
/* eslint-disable @next/next/no-img-element -- uploaded avatar paths are resolved dynamically */

import Link from "next/link";
import type { Profile } from "@/lib/types";
import { useLanguage } from "./language-provider";
import { LinkIcon } from "./link-icon";

export function PublicProfile({ profile }: { profile: Profile | null }) {
  const { dictionary: t } = useLanguage();
  if (!profile) {
    return <main className="center-state public-unavailable"><span className="wordmark">getlink<span>_dtd</span></span><h1>404</h1><p>{t.profile.unavailable}</p></main>;
  }
  const initials = profile.displayName.split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
  return (
    <main className="public-page" data-background-theme={profile.backgroundTheme} data-button-style={profile.buttonStyle} data-font-family={profile.fontFamily}>
      <div className="public-backdrop" />
      <section className="public-card" aria-labelledby="profile-name">
        {/* User-controlled remote URLs cannot share a safe build-time next/image allowlist. */}
        {profile.avatarUrl ? <img className="public-avatar" src={profile.avatarUrl} alt="" /> : <div className="public-avatar avatar-fallback">{initials}</div>}
        <h1 id="profile-name" title={profile.displayName}>{profile.displayName}</h1>
        <p className="public-handle">@{profile.username}</p>
        {profile.bio && <p className="public-bio" title={profile.bio}>{profile.bio}</p>}
        <div className="public-links" aria-label={t.profile.links}>
          {profile.links.map((link) => (
            <a key={link.id} href={`/api/links/r/${link.id}`} target="_blank" rel="noopener noreferrer">
              <LinkIcon icon={link.icon} url={link.destinationUrl} className="public-icon" /><strong title={link.title}>{link.title}</strong><i>↗</i>
            </a>
          ))}
        </div>
        <Link className="powered-by" href="/">{t.profile.poweredBy}</Link>
      </section>
    </main>
  );
}
