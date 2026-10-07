import { PublicProfile } from "@/components/public-profile";
import { LINK_SERVICE_URL } from "@/lib/server-auth";
import type { Profile } from "@/lib/types";

export default async function ProfilePage(props: PageProps<"/[username]">) {
  const { username } = await props.params;
  let profile: Profile | null = null;
  try {
    const response = await fetch(`${LINK_SERVICE_URL}/api/v1/profiles/${encodeURIComponent(username)}`, { cache: "no-store" });
    if (response.ok) profile = (await response.json()) as Profile;
  } catch {
    // The unavailable state also covers a backend that is not running locally.
  }
  return <PublicProfile profile={profile} />;
}
