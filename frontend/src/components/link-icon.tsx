import { resolveLinkIcon } from "@/lib/link-icons";

export function LinkIcon({ icon, url, className = "" }: {
  icon?: string | null;
  url: string;
  className?: string;
}) {
  const descriptor = resolveLinkIcon(icon, url);
  const name = descriptor.kind;

  return (
    <span className={`link-icon ${className}`.trim()} data-icon={name} aria-hidden="true">
      {name === "facebook" && <b>f</b>}
      {name === "instagram" && (
        <svg viewBox="0 0 24 24"><rect x="3.5" y="3.5" width="17" height="17" rx="5" /><circle cx="12" cy="12" r="4" /><circle className="icon-fill" cx="17.5" cy="6.7" r="1" /></svg>
      )}
      {name === "youtube" && (
        <svg viewBox="0 0 24 24"><rect className="icon-fill" x="2" y="5" width="20" height="14" rx="5" /><path className="icon-paper" d="m10 9 6 3-6 3Z" /></svg>
      )}
      {name === "tiktok" && <b>♪</b>}
      {name === "github" && <b className="icon-compact">GH</b>}
      {name === "linkedin" && <b className="icon-compact">in</b>}
      {name === "slack" && (
        <svg viewBox="0 0 24 24">
          <rect className="slack-cyan" x="4" y="9.5" width="7" height="3.5" rx="1.75" />
          <rect className="slack-cyan" x="8" y="4" width="3.5" height="7" rx="1.75" />
          <rect className="slack-green" x="12.5" y="4" width="3.5" height="7" rx="1.75" />
          <rect className="slack-green" x="12.5" y="8" width="7" height="3.5" rx="1.75" />
          <rect className="slack-yellow" x="13" y="12.5" width="7" height="3.5" rx="1.75" />
          <rect className="slack-yellow" x="13" y="13" width="3.5" height="7" rx="1.75" />
          <rect className="slack-red" x="4.5" y="13" width="3.5" height="7" rx="1.75" />
          <rect className="slack-red" x="4.5" y="12.5" width="7" height="3.5" rx="1.75" />
        </svg>
      )}
      {name === "x" && <b>𝕏</b>}
      {name === "globe" && (
        <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5" /><path d="M3.5 12h17M12 3.5c2.2 2.3 3.3 5.2 3.3 8.5S14.2 18.2 12 20.5C9.8 18.2 8.7 15.3 8.7 12S9.8 5.8 12 3.5Z" /></svg>
      )}
      {name === "custom" && <b className="icon-compact">{descriptor.label}</b>}
    </span>
  );
}
