export type Account = {
  id: string;
  username: string;
  email: string | null;
  status: string;
  roles: string[];
};

export type LinkItem = {
  id: string;
  title: string;
  destinationUrl: string;
  icon: string | null;
  position: number;
  enabled: boolean;
  clickCount: number;
  version: number;
};

export type Profile = {
  id: string;
  username: string;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  backgroundTheme: "aurora" | "light" | "sunset" | "midnight";
  buttonStyle: "soft" | "pill" | "square" | "outline";
  fontFamily: "system" | "arial" | "tahoma";
  status: string;
  version: number;
  links: LinkItem[];
};

export type ApiProblem = {
  title?: string;
  detail?: string;
  code?: string;
  status?: number;
};
