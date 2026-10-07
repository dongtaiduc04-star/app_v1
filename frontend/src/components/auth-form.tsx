"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { Account } from "@/lib/types";
import { useLanguage } from "./language-provider";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter();
  const { dictionary: t } = useLanguage();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [password, setPassword] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const payload = {
      username: String(data.get("username") ?? "").trim().toLowerCase(),
      password: String(data.get("password") ?? ""),
      ...(mode === "register" ? { email: String(data.get("email") ?? "").trim() || null } : {}),
    };
    try {
      await api<{ account: Account }>(`/api/session/${mode}`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      router.replace("/dashboard");
      router.refresh();
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : t.auth.genericError);
    } finally {
      setPending(false);
    }
  }

  const isLogin = mode === "login";
  const passwordChecks = isLogin ? [] : [
    { key: "length", valid: password.length >= 10 && password.length <= 25, label: t.auth.passwordRules.length },
    { key: "uppercase", valid: /\p{Lu}/u.test(password), label: t.auth.passwordRules.uppercase },
    { key: "lowercase", valid: /\p{Ll}/u.test(password), label: t.auth.passwordRules.lowercase },
    { key: "number", valid: /\p{N}/u.test(password), label: t.auth.passwordRules.number },
    { key: "symbol", valid: /[^\p{L}\p{N}\s]/u.test(password), label: t.auth.passwordRules.symbol },
  ];
  const passwordIsValid = isLogin || passwordChecks.every((check) => check.valid);

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="auth-title">
        <Link className="wordmark auth-wordmark" href="/">getlink<span>_dtd</span></Link>
        <div className="auth-heading">
          <p className="eyebrow">getlink_dtd</p>
          <h1 id="auth-title">{isLogin ? t.auth.loginTitle : t.auth.registerTitle}</h1>
          <p>{isLogin ? t.auth.loginBody : t.auth.registerBody}</p>
        </div>
        <form onSubmit={submit}>
          <label>
            <span>{t.auth.username}</span>
            <input name="username" autoComplete="username" minLength={3} maxLength={30} pattern="[a-z0-9-]+" required />
          </label>
          {!isLogin && (
            <label>
              <span>{t.auth.email}</span>
              <input name="email" type="email" autoComplete="email" maxLength={254} />
            </label>
          )}
          <label>
            <span>{t.auth.password}</span>
            <input
              name="password"
              type="password"
              autoComplete={isLogin ? "current-password" : "new-password"}
              minLength={isLogin ? 1 : 10}
              maxLength={isLogin ? 128 : 25}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-describedby={isLogin ? undefined : "password-requirements"}
              required
            />
            {!isLogin && (
              <div className="password-requirements" id="password-requirements">
                <small>{t.auth.passwordHint}</small>
                <ul aria-live="polite">
                  {passwordChecks.map((check) => (
                    <li className={check.valid ? "is-valid" : "is-missing"} key={check.key}>
                      <span aria-hidden="true">{check.valid ? "✓" : "○"}</span>
                      {check.label}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="button button-full" disabled={pending || !passwordIsValid} type="submit">
            {pending ? t.auth.working : isLogin ? t.auth.loginAction : t.auth.registerAction}
          </button>
        </form>
        <p className="auth-switch">
          {isLogin ? t.auth.noAccount : t.auth.hasAccount}{" "}
          <Link href={isLogin ? "/register" : "/login"}>
            {isLogin ? t.auth.registerAction : t.auth.loginAction}
          </Link>
        </p>
      </section>
      <aside className="auth-art" aria-hidden="true">
        <div className="orb orb-one" />
        <div className="orb orb-two" />
        <div className="mini-profile">
          <span className="mini-avatar">G</span>
          <strong>@yourname</strong>
          <i /> <i /> <i />
        </div>
      </aside>
    </main>
  );
}
