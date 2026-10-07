import { NextRequest, NextResponse } from "next/server";
import {
  ACCESS_COOKIE,
  LINK_SERVICE_URL,
  applySessionCookies,
  clearSessionCookies,
  problem,
  refreshSession,
} from "@/lib/server-auth";

type Context = { params: Promise<{ path: string[] }> };

async function proxy(request: NextRequest, context: Context) {
  const { path } = await context.params;
  const method = request.method;
  const body = method === "GET" || method === "HEAD" ? undefined : await request.arrayBuffer();

  async function send(token?: string) {
    const headers = new Headers();
    if (token) headers.set("authorization", `Bearer ${token}`);
    const contentType = request.headers.get("content-type");
    if (contentType) headers.set("content-type", contentType);
    return fetch(`${LINK_SERVICE_URL}/api/v1/${path.map(encodeURIComponent).join("/")}`, {
      method,
      headers,
      body,
      cache: "no-store",
      redirect: "manual",
    });
  }

  try {
    let refreshed: Awaited<ReturnType<typeof refreshSession>> = null;
    let upstream = await send(request.cookies.get(ACCESS_COOKIE)?.value);
    if (upstream.status === 401) {
      refreshed = await refreshSession(request);
      if (refreshed) upstream = await send(refreshed.payload.accessToken);
    }

    const headers = new Headers();
    const contentType = upstream.headers.get("content-type");
    const location = upstream.headers.get("location");
    if (contentType) headers.set("content-type", contentType);
    if (location) headers.set("location", location);
    const response = new NextResponse(upstream.status === 204 ? null : await upstream.arrayBuffer(), {
      status: upstream.status,
      headers,
    });
    if (refreshed) applySessionCookies(response, refreshed.payload, refreshed.upstream);
    if (upstream.status === 401 && !refreshed) clearSessionCookies(response);
    return response;
  } catch {
    return problem(503, "LINK_SERVICE_UNAVAILABLE", "Profile service is unavailable");
  }
}

export const GET = proxy;
export const POST = proxy;
export const PATCH = proxy;
export const PUT = proxy;
export const DELETE = proxy;
