export type LinkIconName =
  | "facebook"
  | "instagram"
  | "youtube"
  | "tiktok"
  | "github"
  | "linkedin"
  | "slack"
  | "x"
  | "globe";

export type LinkIconDescriptor = {
  kind: LinkIconName | "custom";
  label?: string;
};

const aliases: Record<string, LinkIconName> = {
  fb: "facebook",
  facebook: "facebook",
  ig: "instagram",
  insta: "instagram",
  instagram: "instagram",
  yt: "youtube",
  youtube: "youtube",
  tiktok: "tiktok",
  github: "github",
  gh: "github",
  linkedin: "linkedin",
  li: "linkedin",
  slack: "slack",
  twitter: "x",
  x: "x",
  website: "globe",
  web: "globe",
  globe: "globe",
};

const hosts: Array<[LinkIconName, string[]]> = [
  ["facebook", ["facebook.com", "fb.com", "fb.me"]],
  ["instagram", ["instagram.com"]],
  ["youtube", ["youtube.com", "youtu.be"]],
  ["tiktok", ["tiktok.com"]],
  ["github", ["github.com"]],
  ["linkedin", ["linkedin.com", "linked.in"]],
  ["slack", ["slack.com"]],
  ["x", ["x.com", "twitter.com"]],
];

const legacyAutomaticValues = new Set<LinkIconName>([
  ...hosts.map(([name]) => name),
  "globe",
]);

function hostname(value: string) {
  const candidate = value.trim();
  if (!candidate) return "";

  try {
    const url = new URL(candidate.includes("://") ? candidate : `https://${candidate}`);
    return url.hostname.toLowerCase().replace(/^www\./, "");
  } catch {
    return "";
  }
}

export function detectLinkIcon(destinationUrl: string): LinkIconName {
  const host = hostname(destinationUrl);
  const match = hosts.find(([, domains]) =>
    domains.some((domain) => host === domain || host.endsWith(`.${domain}`)),
  );
  return match?.[0] ?? "globe";
}

// Earlier frontend versions stored automatically detected icon names in the
// database. Treat only those canonical values as automatic when a link is
// edited, so changing its URL also refreshes the preview and saved icon.
export function editableCustomIconValue(icon: string | null | undefined) {
  const value = icon?.trim() ?? "";
  if (legacyAutomaticValues.has(value.toLowerCase() as LinkIconName)) return "";
  return value;
}

export function resolveLinkIcon(icon: string | null | undefined, destinationUrl: string): LinkIconDescriptor {
  const manual = icon?.trim();
  if (!manual) return { kind: detectLinkIcon(destinationUrl) };

  const known = aliases[manual.toLowerCase()];
  if (known) return { kind: known };

  return { kind: "custom", label: manual.slice(0, 2).toUpperCase() };
}
