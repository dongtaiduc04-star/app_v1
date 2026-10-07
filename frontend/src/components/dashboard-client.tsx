"use client";
/* eslint-disable @next/next/no-img-element -- avatar previews may use local blob URLs or uploaded image paths */

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { CONTENT_LIMITS } from "@/lib/content-limits";
import { editableCustomIconValue } from "@/lib/link-icons";
import type { LinkItem, Profile } from "@/lib/types";
import { LinkIcon } from "./link-icon";
import { useLanguage } from "./language-provider";
import { SiteHeader } from "./site-header";

type LinkDraft = Pick<LinkItem, "title" | "destinationUrl" | "icon" | "enabled">;

export function DashboardClient() {
  const router = useRouter();
  const { dictionary: t } = useLanguage();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState<string | null>(null);
  const [dragging, setDragging] = useState<string | null>(null);
  const [newLinkUrl, setNewLinkUrl] = useState("");
  const [newLinkIcon, setNewLinkIcon] = useState("");
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState("");
  const [backgroundTheme, setBackgroundTheme] = useState<Profile["backgroundTheme"]>("aurora");
  const [buttonStyle, setButtonStyle] = useState<Profile["buttonStyle"]>("soft");
  const [fontFamily, setFontFamily] = useState<Profile["fontFamily"]>("system");

  async function load() {
    try {
      const value = await api<Profile>("/api/links/me/profile");
      setProfile(value);
      setBackgroundTheme(value.backgroundTheme);
      setButtonStyle(value.buttonStyle);
      setFontFamily(value.fontFamily);
    } catch (reason) {
      if (reason instanceof ApiError && reason.status === 401) router.replace("/login");
      else setError(reason instanceof Error ? reason.message : t.auth.genericError);
    }
  }

  useEffect(() => {
    let active = true;
    api<Profile>("/api/links/me/profile")
      .then((value) => {
        if (active) {
          setProfile(value);
          setBackgroundTheme(value.backgroundTheme);
          setButtonStyle(value.buttonStyle);
          setFontFamily(value.fontFamily);
        }
      })
      .catch((reason: unknown) => {
        if (!active) return;
        if (reason instanceof ApiError && reason.status === 401) router.replace("/login");
        else setError(reason instanceof Error ? reason.message : t.auth.genericError);
      });
    return () => { active = false; };
  }, [router, t.auth.genericError]);

  useEffect(() => {
    return () => {
      if (avatarPreview.startsWith("blob:")) URL.revokeObjectURL(avatarPreview);
    };
  }, [avatarPreview]);

  function flash(message: string) {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 2200);
  }

  function selectAvatar(file?: File) {
    setError("");
    if (!file) {
      setAvatarFile(null);
      setAvatarPreview("");
      return;
    }
    const allowed = ["image/png", "image/jpeg", "image/webp", "image/gif"];
    if (!allowed.includes(file.type) || file.size > 5 * 1024 * 1024) {
      setError(t.dashboard.avatarInvalid);
      return;
    }
    setAvatarFile(file);
    setAvatarPreview(URL.createObjectURL(file));
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      const updated = await api<Profile>("/api/links/me/profile", {
        method: "PATCH",
        body: JSON.stringify({
          displayName: String(data.get("displayName") ?? ""),
          bio: String(data.get("bio") ?? ""),
          backgroundTheme,
          buttonStyle,
          fontFamily,
        }),
      });
      let finalProfile = updated;
      if (avatarFile) {
        const upload = new FormData();
        upload.set("file", avatarFile);
        finalProfile = await api<Profile>("/api/links/me/profile/avatar", { method: "POST", body: upload });
      }
      setProfile(finalProfile);
      setBackgroundTheme(finalProfile.backgroundTheme);
      setButtonStyle(finalProfile.buttonStyle);
      setFontFamily(finalProfile.fontFamily);
      setAvatarFile(null);
      setAvatarPreview("");
      flash(t.dashboard.saved);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t.auth.genericError);
    } finally { setBusy(false); }
  }

  async function addLink(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(true);
    setError("");
    try {
      const created = await api<LinkItem>("/api/links/me/links", {
        method: "POST",
        body: JSON.stringify({
          title: String(data.get("title") ?? ""),
          destinationUrl: newLinkUrl,
          icon: newLinkIcon.trim() || null,
        }),
      });
      setProfile((current) => current ? { ...current, links: [...current.links, created] } : current);
      form.reset();
      setNewLinkUrl("");
      setNewLinkIcon("");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t.auth.genericError);
    } finally { setBusy(false); }
  }

  async function updateLink(link: LinkItem, draft: LinkDraft) {
    setBusy(true);
    setError("");
    try {
      const updated = await api<LinkItem>(`/api/links/me/links/${link.id}`, {
        method: "PATCH",
        body: JSON.stringify(draft),
      });
      setProfile((current) => current ? {
        ...current,
        links: current.links.map((item) => item.id === updated.id ? updated : item),
      } : current);
      setEditing(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t.auth.genericError);
    } finally { setBusy(false); }
  }

  async function removeLink(linkId: string) {
    if (!window.confirm(t.dashboard.confirmDelete)) return;
    setBusy(true);
    try {
      await api<void>(`/api/links/me/links/${linkId}`, { method: "DELETE" });
      setProfile((current) => current ? {
        ...current,
        links: current.links.filter((item) => item.id !== linkId)
          .map((item, position) => ({ ...item, position })),
      } : current);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t.auth.genericError);
    } finally { setBusy(false); }
  }

  async function reorder(fromId: string, toIndex: number) {
    if (!profile) return;
    const currentIndex = profile.links.findIndex((item) => item.id === fromId);
    if (currentIndex < 0 || toIndex < 0 || toIndex >= profile.links.length || currentIndex === toIndex) return;
    const ordered = [...profile.links];
    const [moved] = ordered.splice(currentIndex, 1);
    ordered.splice(toIndex, 0, moved);
    setProfile({ ...profile, links: ordered.map((item, position) => ({ ...item, position })) });
    try {
      const saved = await api<LinkItem[]>("/api/links/me/links/order", {
        method: "PUT",
        body: JSON.stringify({ linkIds: ordered.map((item) => item.id) }),
      });
      setProfile((value) => value ? { ...value, links: saved } : value);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : t.auth.genericError);
      await load();
    }
  }

  async function logout() {
    await api<void>("/api/session/logout", { method: "POST" }).catch(() => undefined);
    router.replace("/");
    router.refresh();
  }

  if (!profile) {
    return <main className="center-state"><span className="loader" /><p>{error || t.dashboard.loading}</p></main>;
  }

  return (
    <div className="dashboard-shell">
      <SiteHeader dashboard />
      <main className="dashboard-main">
        <header className="dashboard-intro">
          <div><p className="eyebrow">@{profile.username}</p><h1>{t.dashboard.title}</h1><p>{t.dashboard.subtitle}</p></div>
          <div className="dashboard-actions">
            <Link className="button button-secondary" href={`/${profile.username}`} target="_blank">{t.dashboard.preview}</Link>
            <button className="text-button" type="button" onClick={logout}>{t.dashboard.logout}</button>
          </div>
        </header>

        {error && <p className="form-error global-message" role="alert">{error}</p>}
        {notice && <p className="success-message" role="status">{notice}</p>}

        <div className="dashboard-grid">
          <section className="panel profile-panel" aria-labelledby="profile-heading">
            <div className="panel-heading"><h2 id="profile-heading">{t.dashboard.profile}</h2></div>
            <form onSubmit={saveProfile}>
              <div className="avatar-editor">
                {(avatarPreview || profile.avatarUrl) ? <img src={avatarPreview || profile.avatarUrl || ""} alt={t.dashboard.avatarPreview} /> : <span>{profile.displayName.slice(0, 2).toUpperCase()}</span>}
                <label className="avatar-picker">
                  <span>{t.dashboard.uploadAvatar}</span>
                  <input type="file" accept="image/png,image/jpeg,image/webp,image/gif" onChange={(event) => selectAvatar(event.target.files?.[0])} />
                </label>
                <small>{avatarFile?.name || t.dashboard.avatarHint}</small>
              </div>
              <label><span>{t.dashboard.displayName}</span><input name="displayName" defaultValue={profile.displayName} maxLength={CONTENT_LIMITS.displayName} required /></label>
              <label><span>{t.dashboard.bio}</span><textarea name="bio" defaultValue={profile.bio ?? ""} maxLength={CONTENT_LIMITS.bio} rows={4} /></label>
              <button className="button" disabled={busy} type="submit">{t.dashboard.saveProfile}</button>
            </form>
          </section>

          <section className="panel links-panel" aria-labelledby="links-heading">
            <div className="panel-heading"><div><h2 id="links-heading">{t.dashboard.links}</h2><p>{t.dashboard.dragHint}</p></div></div>
            <form className="add-link-form" onSubmit={addLink}>
              <input name="title" aria-label={t.dashboard.linkTitle} placeholder={t.dashboard.linkTitle} maxLength={CONTENT_LIMITS.linkTitle} required />
              <input name="destinationUrl" aria-label={t.dashboard.linkUrl} type="url" placeholder={t.dashboard.linkUrl} maxLength={CONTENT_LIMITS.url} required value={newLinkUrl} onChange={(event) => setNewLinkUrl(event.target.value)} />
              <div className="icon-field">
                <LinkIcon icon={newLinkIcon} url={newLinkUrl} className="icon-preview" />
                <input name="icon" aria-label={t.dashboard.icon} placeholder={t.dashboard.icon} maxLength={CONTENT_LIMITS.customIcon} value={newLinkIcon} onChange={(event) => setNewLinkIcon(event.target.value)} />
                <small>{newLinkIcon.trim() ? t.dashboard.iconCustom : t.dashboard.iconAuto}</small>
              </div>
              <button className="button" disabled={busy || profile.links.length >= 30} type="submit">+ {t.dashboard.addLink}</button>
            </form>

            <div className="link-list">
              {profile.links.length === 0 && <p className="empty-state">{t.dashboard.empty}</p>}
              {profile.links.map((link, index) => (
                <LinkEditor
                  key={link.id}
                  link={link}
                  index={index}
                  total={profile.links.length}
                  editing={editing === link.id}
                  busy={busy}
                  labels={t.dashboard}
                  onEdit={() => setEditing(link.id)}
                  onCancel={() => setEditing(null)}
                  onSave={(draft) => updateLink(link, draft)}
                  onToggle={() => updateLink(link, { title: link.title, destinationUrl: link.destinationUrl, icon: link.icon, enabled: !link.enabled })}
                  onRemove={() => removeLink(link.id)}
                  onMove={(next) => reorder(link.id, next)}
                  onDragStart={() => setDragging(link.id)}
                  onDrop={() => { if (dragging) void reorder(dragging, index); setDragging(null); }}
                />
              ))}
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}

function LinkEditor({ link, index, total, editing, busy, labels, onEdit, onCancel, onSave, onToggle, onRemove, onMove, onDragStart, onDrop }: {
  link: LinkItem; index: number; total: number; editing: boolean; busy: boolean;
  labels: ReturnType<typeof useLanguage>["dictionary"]["dashboard"];
  onEdit: () => void; onCancel: () => void; onSave: (draft: LinkDraft) => void;
  onToggle: () => void; onRemove: () => void; onMove: (index: number) => void;
  onDragStart: () => void; onDrop: () => void;
}) {
  return (
    <article className={`link-editor ${link.enabled ? "" : "is-disabled"}`} draggable={!editing} onDragStart={onDragStart} onDragOver={(event) => event.preventDefault()} onDrop={onDrop}>
      <span className="drag-handle" aria-hidden="true">⠿</span>
      {editing ? (
        <LinkEditForm link={link} busy={busy} labels={labels} onCancel={onCancel} onSave={onSave} />
      ) : (
        <>
          <LinkIcon icon={link.icon} url={link.destinationUrl} className="link-card-icon" />
          <div className="link-copy"><strong title={link.title}>{link.title}</strong><span title={link.destinationUrl}>{link.destinationUrl}</span><small>{link.clickCount} {labels.clicks}</small></div>
          <div className="link-controls">
            <button className="icon-button" type="button" disabled={index === 0 || busy} onClick={() => onMove(index - 1)} aria-label={labels.moveUp}>↑</button>
            <button className="icon-button" type="button" disabled={index === total - 1 || busy} onClick={() => onMove(index + 1)} aria-label={labels.moveDown}>↓</button>
            <button className={`visibility-toggle ${link.enabled ? "active" : ""}`} type="button" onClick={onToggle}>{link.enabled ? labels.enabled : labels.disabled}</button>
            <button className="text-button" type="button" onClick={onEdit}>{labels.edit}</button>
            <button className="text-button danger" type="button" onClick={onRemove}>{labels.remove}</button>
          </div>
        </>
      )}
    </article>
  );
}

function LinkEditForm({ link, busy, labels, onCancel, onSave }: {
  link: LinkItem;
  busy: boolean;
  labels: ReturnType<typeof useLanguage>["dictionary"]["dashboard"];
  onCancel: () => void;
  onSave: (draft: LinkDraft) => void;
}) {
  const [destinationUrl, setDestinationUrl] = useState(link.destinationUrl);
  const [icon, setIcon] = useState(editableCustomIconValue(link.icon));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    onSave({
      title: String(data.get("title")),
      destinationUrl,
      icon: icon.trim() || null,
      enabled: link.enabled,
    });
  }

  return (
    <form className="edit-link-form" onSubmit={submit}>
      <input name="title" defaultValue={link.title} maxLength={CONTENT_LIMITS.linkTitle} required aria-label={labels.linkTitle} />
      <input name="destinationUrl" value={destinationUrl} onChange={(event) => setDestinationUrl(event.target.value)} type="url" maxLength={CONTENT_LIMITS.url} required aria-label={labels.linkUrl} />
      <div className="icon-field">
        <LinkIcon icon={icon} url={destinationUrl} className="icon-preview" />
        <input name="icon" value={icon} onChange={(event) => setIcon(event.target.value)} maxLength={CONTENT_LIMITS.customIcon} aria-label={labels.icon} />
        <small>{icon.trim() ? labels.iconCustom : labels.iconAuto}</small>
      </div>
      <div className="edit-actions"><button className="button button-small" disabled={busy}>{labels.save}</button><button className="text-button" type="button" onClick={onCancel}>{labels.cancel}</button></div>
    </form>
  );
}
