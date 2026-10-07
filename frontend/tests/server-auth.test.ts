import assert from "node:assert/strict";
import { afterEach, test } from "node:test";

import { NextRequest, NextResponse } from "next/server";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  applySessionCookies,
  clearSessionCookies,
  getRefreshCookieValue,
  problem,
  refreshSession,
} from "../src/lib/server-auth";

const originalFetch = globalThis.fetch;
const originalNodeEnv = process.env.NODE_ENV;

afterEach(() => {
  globalThis.fetch = originalFetch;
  setNodeEnv(originalNodeEnv);
});

function setNodeEnv(value: string | undefined) {
  if (value === undefined) {
    Reflect.deleteProperty(process.env, "NODE_ENV");
    return;
  }
  Object.assign(process.env, { NODE_ENV: value });
}

const account = {
  id: "account-1",
  username: "alice",
  email: "alice@example.com",
  status: "ACTIVE",
  roles: ["USER"],
};

test("getRefreshCookieValue reads only the refresh cookie", () => {
  assert.equal(getRefreshCookieValue(new Response()), null);
  assert.equal(
    getRefreshCookieValue(
      new Response(null, { headers: { "set-cookie": "other=value; Path=/" } }),
    ),
    null,
  );
  assert.equal(
    getRefreshCookieValue(
      new Response(null, {
        headers: {
          "set-cookie": "other=value; Path=/, getlink_refresh=refresh-token; Path=/api; HttpOnly",
        },
      }),
    ),
    "refresh-token",
  );
});

test("applySessionCookies creates HttpOnly access and rotated refresh cookies", () => {
  setNodeEnv("production");
  const response = NextResponse.json({ ok: true });
  const upstream = new Response(null, {
    headers: { "set-cookie": "getlink_refresh=rotated; Path=/api; HttpOnly" },
  });

  applySessionCookies(
    response,
    { accessToken: "access-token", expiresIn: 600, account },
    upstream,
  );

  const cookies = response.headers.get("set-cookie") ?? "";
  assert.match(cookies, /getlink_access=access-token/);
  assert.match(cookies, /getlink_refresh=rotated/);
  assert.match(cookies, /HttpOnly/);
  assert.match(cookies, /Secure/);
  assert.match(cookies, /SameSite=lax/);
  assert.match(cookies, /SameSite=strict/);
});

test("applySessionCookies works without refresh rotation in development", () => {
  setNodeEnv("development");
  const response = NextResponse.json({ ok: true });

  applySessionCookies(
    response,
    { accessToken: "access-token", expiresIn: 600, account },
    new Response(),
  );

  const cookies = response.headers.get("set-cookie") ?? "";
  assert.match(cookies, /getlink_access=access-token/);
  assert.doesNotMatch(cookies, /getlink_refresh=/);
  assert.doesNotMatch(cookies, /Secure/);
});

test("cookie clearing uses the expected names and paths", () => {
  const sessionResponse = NextResponse.json({ ok: true });
  clearSessionCookies(sessionResponse);
  const sessionCookies = sessionResponse.headers.get("set-cookie") ?? "";
  assert.match(sessionCookies, new RegExp(`${ACCESS_COOKIE}=`));
  assert.match(sessionCookies, new RegExp(`${REFRESH_COOKIE}=`));
  assert.match(sessionCookies, /Path=\//);
  assert.match(sessionCookies, /Path=\/api/);
});

test("refreshSession handles missing, rejected, and valid refresh sessions", async (context) => {
  await context.test("returns null without a refresh cookie", async () => {
    globalThis.fetch = (async () => assert.fail("fetch must not run")) as typeof fetch;
    assert.equal(await refreshSession(new NextRequest("http://localhost:3000/dashboard")), null);
  });

  await context.test("returns null when refresh is rejected", async () => {
    globalThis.fetch = (async () => new Response(null, { status: 401 })) as typeof fetch;
    const result = await refreshSession(
      new NextRequest("http://localhost:3000/dashboard", {
        headers: { cookie: `${REFRESH_COOKIE}=refresh-token` },
      }),
    );
    assert.equal(result, null);
  });

  await context.test("returns the refreshed payload and upstream response", async () => {
    let receivedCookie = "";
    globalThis.fetch = (async (_input: RequestInfo | URL, init?: RequestInit) => {
      receivedCookie = (init?.headers as Record<string, string>).cookie;
      return Response.json({ accessToken: "new-access", expiresIn: 900, account });
    }) as typeof fetch;
    const result = await refreshSession(
      new NextRequest("http://localhost:3000/dashboard", {
        headers: { cookie: `${REFRESH_COOKIE}=refresh-token` },
      }),
    );
    assert.equal(receivedCookie, `${REFRESH_COOKIE}=refresh-token`);
    assert.equal(result?.payload.accessToken, "new-access");
    assert.equal(result?.upstream.status, 200);
  });
});

test("problem creates an RFC 7807 style response", async () => {
  const response = problem(409, "PROFILE_CONFLICT", "Profile update conflicts");
  assert.equal(response.status, 409);
  assert.equal(response.headers.get("content-type"), "application/problem+json");
  assert.deepEqual(await response.json(), {
    title: "Request failed",
    status: 409,
    code: "PROFILE_CONFLICT",
    detail: "Profile update conflicts",
  });
});
