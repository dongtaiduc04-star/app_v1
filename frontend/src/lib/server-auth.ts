import "server-only";

import { NextRequest, NextResponse } from "next/server";
import type { Account } from "./types";

export const ACCESS_COOKIE = "getlink_access";
export const REFRESH_COOKIE = "getlink_refresh";
export const API_GATEWAY_URL = process.env.API_GATEWAY_URL ?? "http://localhost:8080";
export const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL ?? API_GATEWAY_URL;
export const LINK_SERVICE_URL = process.env.LINK_SERVICE_URL ?? API_GATEWAY_URL;

export type AuthPayload = {
  accessToken: string;
  expiresIn: number;
  account: Account;
};

export function getRefreshCookieValue(response: Response) {
  const header = response.headers.get("set-cookie");
  if (!header) return null;
  const match = header.match(new RegExp(`(?:^|,\\s*)${REFRESH_COOKIE}=([^;,]+)`));
  return match?.[1] ?? null;
}

export function applySessionCookies(
  response: NextResponse,
  payload: AuthPayload,
  upstream: Response,
) {
  const secure = process.env.NODE_ENV === "production";
  response.cookies.set(ACCESS_COOKIE, payload.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    maxAge: payload.expiresIn,
    path: "/",
  });
  const refreshToken = getRefreshCookieValue(upstream);
  if (refreshToken) {
    response.cookies.set(REFRESH_COOKIE, refreshToken, {
      httpOnly: true,
      sameSite: "strict",
      secure,
      maxAge: 60 * 60 * 24 * 30,
      path: "/api",
    });
  }
}

export function clearSessionCookies(response: NextResponse) {
  response.cookies.set(ACCESS_COOKIE, "", { httpOnly: true, maxAge: 0, path: "/" });
  response.cookies.set(REFRESH_COOKIE, "", { httpOnly: true, maxAge: 0, path: "/api" });
}

export async function refreshSession(request: NextRequest) {
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  if (!refresh) return null;
  const upstream = await fetch(`${AUTH_SERVICE_URL}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { cookie: `${REFRESH_COOKIE}=${refresh}` },
    cache: "no-store",
  });
  if (!upstream.ok) return null;
  return { payload: (await upstream.json()) as AuthPayload, upstream };
}

export function problem(status: number, code: string, detail: string) {
  return NextResponse.json(
    { title: "Request failed", status, code, detail },
    { status, headers: { "content-type": "application/problem+json" } },
  );
}
