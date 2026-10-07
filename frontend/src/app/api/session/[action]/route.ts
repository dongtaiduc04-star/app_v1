import { NextRequest, NextResponse } from "next/server";
import {
  ACCESS_COOKIE,
  AUTH_SERVICE_URL,
  REFRESH_COOKIE,
  applySessionCookies,
  clearSessionCookies,
  problem,
} from "@/lib/server-auth";
import type { Account } from "@/lib/types";

type AuthPayload = { accessToken: string; expiresIn: number; account: Account };

export async function POST(
  request: NextRequest,
  context: { params: Promise<{ action: string }> },
) {
  const { action } = await context.params;
  if (!["login", "register", "refresh", "logout"].includes(action)) {
    return problem(404, "NOT_FOUND", "Unknown session action");
  }

  try {
    const headers = new Headers();
    const access = request.cookies.get(ACCESS_COOKIE)?.value;
    const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
    if (access) headers.set("authorization", `Bearer ${access}`);
    if (refresh) headers.set("cookie", `${REFRESH_COOKIE}=${refresh}`);
    if (action === "login" || action === "register") headers.set("content-type", "application/json");

    const upstream = await fetch(`${AUTH_SERVICE_URL}/api/v1/auth/${action}`, {
      method: "POST",
      headers,
      body: action === "login" || action === "register" ? await request.text() : undefined,
      cache: "no-store",
      redirect: "manual",
    });

    if (!upstream.ok) {
      return new NextResponse(await upstream.text(), {
        status: upstream.status,
        headers: { "content-type": upstream.headers.get("content-type") ?? "application/problem+json" },
      });
    }

    if (action === "logout") {
      const response = new NextResponse(null, { status: 204 });
      clearSessionCookies(response);
      return response;
    }

    const payload = (await upstream.json()) as AuthPayload;
    const response = NextResponse.json({ account: payload.account });
    applySessionCookies(response, payload, upstream);
    return response;
  } catch {
    return problem(503, "AUTH_SERVICE_UNAVAILABLE", "Authentication service is unavailable");
  }
}
