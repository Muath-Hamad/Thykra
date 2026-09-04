import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useNavigate, useParams } from '@tanstack/react-router';
import { MoreHorizontal } from 'lucide-react';
import {
  addMember,
  blockMember,
  createInviteLink,
  deleteAlbum,
  getAlbum,
  getBlockedMembers,
  getMembers,
  removeMember,
  unblockMember,
  updateAlbum,
  type AlbumDto,
  type AlbumMemberDto,
  type BlockedMemberDto,
} from '../api/albums';
import { listInvites, revokeInvite, type InviteLinkDto } from '../api/invites';
import { getAlbumMedia, type MediaDto } from '../api/media';
import { useAuth } from '../auth/AuthContext';
import { useLocale } from '../i18n/LocaleProvider';
import { MobileContextBar, useMobileContextBar } from '../app/AppShell';
import { formatDateMedium } from '../lib/format';
import { Avatar } from '../ui/Avatar';
import { Badge, type BadgeKind } from '../ui/Badge';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { Input, Select, Textarea } from '../ui/Field';
import { Menu } from '../ui/Menu';
import { Confirm } from '../ui/Modal';
import { Delayed, ListSkeleton, Skeleton } from '../ui/Skeleton';
import { useToast } from '../ui/Toast';
import { useDocumentTitle, useMediaQuery } from '../ui/hooks';
import styles from './tripSettings.module.css';

type SectionId = 'details' | 'privacy' | 'people' | 'invites' | 'blocked' | 'danger' | 'leave';

interface RailEntry {
  id: SectionId;
  label: string;
  count?: number;
  danger?: boolean;
}

type Dialog =
  | { kind: 'privacy' }
  | { kind: 'remove'; member: AlbumMemberDto }
  | { kind: 'block'; member: AlbumMemberDto }
  | { kind: 'revoke'; token: string }
  | { kind: 'delete' }
  | { kind: 'leave' }
  | null;

const ROLE_BADGE: Record<AlbumMemberDto['role'], BadgeKind> = {
  OWNER: 'owner',
  CONTRIBUTOR: 'contributor',
  VIEWER: 'viewer',
};

function prefersReducedMotionNow(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function TripSettingsPage() {
  const { albumId } = useParams({ strict: false }) as { albumId: string };
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t, lang } = useLocale();
  const { toast } = useToast();
  const isMobile = useMediaQuery('(max-width: 767px)');

  // ── Server truth ──
  const [album, setAlbum] = useState<AlbumDto | null>(null);
  const [albumError, setAlbumError] = useState<'forbidden' | 'error' | null>(null);
  const [members, setMembers] = useState<AlbumMemberDto[] | null>(null);
  const [media, setMedia] = useState<MediaDto[] | null>(null);
  const [invites, setInvites] = useState<InviteLinkDto[] | null>(null);
  const [blocked, setBlocked] = useState<BlockedMemberDto[] | null>(null);

  const role = useMemo(() => {
    if (!members || !user) return null;
    return members.find((m) => m.userId === user.id)?.role ?? null;
  }, [members, user]);
  const roleKnown = members !== null && user !== null;
  const isOwner = role === 'OWNER';

  useDocumentTitle(album ? `${album.title} · ${t('settings.title')} · Thykra` : null);

  // ── Fetch ──
  const loadAlbum = useCallback(async () => {
    setAlbumError(null);
    const resp = await getAlbum(albumId).catch(() => null);
    if (resp?.success && resp.data) setAlbum(resp.data);
    else if (resp && !resp.success) setAlbumError('forbidden');
    else setAlbumError('error');
  }, [albumId]);

  const loadMembers = useCallback(async () => {
    const resp = await getMembers(albumId).catch(() => null);
    if (resp?.success && resp.data) setMembers(resp.data);
  }, [albumId]);

  const loadInvites = useCallback(async () => {
    const resp = await listInvites(albumId).catch(() => null);
    if (resp?.success && resp.data) setInvites(resp.data);
    else setInvites([]);
  }, [albumId]);

  const loadBlocked = useCallback(async () => {
    const resp = await getBlockedMembers(albumId).catch(() => null);
    if (resp?.success && resp.data) setBlocked(resp.data);
    else setBlocked([]);
  }, [albumId]);

  useEffect(() => {
    void loadAlbum();
    void loadMembers();
    getAlbumMedia(albumId).then(
      (resp) => {
        if (resp.success && resp.data) setMedia(resp.data);
        else setMedia([]);
      },
      () => setMedia([]),
    );
  }, [albumId, loadAlbum, loadMembers]);

  // Owner-only collections — never requested for a contributor or viewer.
  useEffect(() => {
    if (!isOwner) return;
    void loadInvites();
    void loadBlocked();
  }, [isOwner, loadInvites, loadBlocked]);

  const uploadCounts = useMemo(() => {
    const counts = new Map<string, number>();
    for (const item of media ?? []) {
      counts.set(item.uploaderId, (counts.get(item.uploaderId) ?? 0) + 1);
    }
    return counts;
  }, [media]);
  const photoCount = media?.length ?? 0;
  const memberCount = members?.length ?? album?.memberCount ?? 0;

  // ── Sections + scroll spy ──
  const rail = useMemo<RailEntry[]>(() => {
    const entries: RailEntry[] = [{ id: 'details', label: t('settings.details') }];
    if (isOwner) entries.push({ id: 'privacy', label: t('settings.privacy') });
    entries.push({
      id: 'people',
      label: t('settings.people'),
      count: members?.length,
    });
    if (isOwner) {
      entries.push({ id: 'invites', label: t('settings.invites'), count: invites?.length });
      if (blocked && blocked.length > 0) {
        entries.push({ id: 'blocked', label: t('settings.blocked'), count: blocked.length });
      }
      entries.push({ id: 'danger', label: t('settings.danger'), danger: true });
    } else if (roleKnown) {
      entries.push({ id: 'leave', label: t('trip.leave'), danger: true });
    }
    return entries;
  }, [isOwner, roleKnown, members, invites, blocked, t]);

  const [active, setActive] = useState<SectionId>('details');
  const railKey = rail.map((r) => r.id).join(',');

  useEffect(() => {
    const ids = railKey.split(',') as SectionId[];
    const nodes = ids
      .map((id) => document.getElementById(id))
      .filter((el): el is HTMLElement => el !== null);
    if (nodes.length === 0) return;
    const seen = new Map<string, boolean>();
    // One observer for every section; the topmost intersecting one wins.
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) seen.set(entry.target.id, entry.isIntersecting);
        const first = ids.find((id) => seen.get(id));
        if (first) setActive(first);
      },
      { rootMargin: '-88px 0px -55% 0px', threshold: 0 },
    );
    nodes.forEach((node) => observer.observe(node));
    return () => observer.disconnect();
  }, [railKey]);

  const goToSection = useCallback((id: SectionId) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.scrollIntoView({
      behavior: prefersReducedMotionNow() ? 'auto' : 'smooth',
      block: 'start',
    });
    setActive(id);
    // replaceState rather than location.hash = … so the browser's own instant
    // jump never races the smooth scroll.
    window.history.replaceState(null, '', `#${id}`);
  }, []);

  // Deep link on first paint.
  const hashHandled = useRef(false);
  useEffect(() => {
    if (hashHandled.current || !album) return;
    const id = window.location.hash.replace('#', '');
    if (!id) {
      hashHandled.current = true;
      return;
    }
    // Owner-gated sections only mount once members load, so the target may not
    // exist yet — stay unhandled and retry when they do.
    const handle = window.setTimeout(() => {
      const el = document.getElementById(id);
      if (!el) return;
      hashHandled.current = true;
      el.scrollIntoView({ behavior: 'auto', block: 'start' });
    }, 0);
    return () => window.clearTimeout(handle);
  }, [album, members, isOwner]);

  // ── Details form ──
  const [form, setForm] = useState<{ title: string; description: string } | null>(null);
  const [nameError, setNameError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (album && form === null) {
      setForm({ title: album.title, description: album.description ?? '' });
    }
  }, [album, form]);

  const dirty =
    !!album &&
    !!form &&
    (form.title !== album.title || form.description !== (album.description ?? ''));

  const saveDetails = useCallback(async () => {
    if (!album || !form) return;
    if (form.title.trim().length === 0) {
      setNameError(t('settings.details.nameRequired'));
      return;
    }
    setNameError(null);
    setSaving(true);
    const resp = await updateAlbum(albumId, {
      title: form.title.trim(),
      description: form.description,
    }).catch(() => null);
    setSaving(false);
    if (resp?.success && resp.data) {
      setAlbum(resp.data);
      setForm({ title: resp.data.title, description: resp.data.description ?? '' });
      toast({ kind: 'success', title: t('settings.details.saved') });
    } else {
      // Edits are kept — the fields are still exactly what the user typed.
      toast({ kind: 'error', title: t('settings.details.saveFailed') });
    }
  }, [album, form, albumId, t, toast]);

  // ── Privacy ──
  const [visibilityBusy, setVisibilityBusy] = useState(false);
  const shareUrl = `${window.location.origin}/s/${albumId}`;

  const applyVisibility = useCallback(
    async (visibility: AlbumDto['visibility']) => {
      setVisibilityBusy(true);
      const resp = await updateAlbum(albumId, { visibility }).catch(() => null);
      setVisibilityBusy(false);
      if (resp?.success && resp.data) setAlbum(resp.data);
      else toast({ kind: 'error', title: t('settings.details.saveFailed') });
      return !!resp?.success;
    },
    [albumId, t, toast],
  );

  const copyText = useCallback(
    async (text: string, title: string, body?: string) => {
      try {
        await navigator.clipboard.writeText(text);
        toast({ kind: 'success', title, body });
      } catch {
        toast({ kind: 'error', title: t('common.errorGeneric') });
      }
    },
    [t, toast],
  );

  // ── Invites ──
  const [expiresIn, setExpiresIn] = useState('7');
  const [creating, setCreating] = useState(false);
  const [busyToken, setBusyToken] = useState<string | null>(null);

  const createLink = useCallback(async () => {
    setCreating(true);
    const resp = await createInviteLink(albumId, Number(expiresIn)).catch(() => null);
    if (resp?.success) await loadInvites();
    else toast({ kind: 'error', title: t('common.errorGeneric') });
    setCreating(false);
  }, [albumId, expiresIn, loadInvites, t, toast]);

  const regenerate = useCallback(
    async (token: string) => {
      setBusyToken(token);
      const revoked = await revokeInvite(albumId, token).catch(() => null);
      if (revoked?.success) {
        const created = await createInviteLink(albumId, Number(expiresIn)).catch(() => null);
        if (!created?.success) toast({ kind: 'error', title: t('common.errorGeneric') });
      } else {
        toast({ kind: 'error', title: t('common.errorGeneric') });
      }
      await loadInvites();
      setBusyToken(null);
    },
    [albumId, expiresIn, loadInvites, t, toast],
  );

  // ── Destructive flows ──
  const [dialog, setDialog] = useState<Dialog>(null);
  const [dialogBusy, setDialogBusy] = useState(false);

  const closeDialog = useCallback(() => {
    if (!dialogBusy) setDialog(null);
  }, [dialogBusy]);

  const doRemove = useCallback(
    async (member: AlbumMemberDto) => {
      setDialogBusy(true);
      const resp = await removeMember(albumId, member.userId).catch(() => null);
      setDialogBusy(false);
      if (!resp?.success) {
        toast({ kind: 'error', title: t('common.errorGeneric') });
        return;
      }
      setDialog(null);
      await loadMembers();
      toast({
        kind: 'warm',
        title: t('settings.remove.toast', { name: member.displayName }),
        action: {
          label: t('common.undo'),
          onClick: () => {
            void addMember(albumId, member.userId, member.role)
              .catch(() => null)
              .then(() => loadMembers());
          },
        },
      });
    },
    [albumId, loadMembers, t, toast],
  );

  const doBlock = useCallback(
    async (member: AlbumMemberDto) => {
      setDialogBusy(true);
      const resp = await blockMember(albumId, member.userId).catch(() => null);
      setDialogBusy(false);
      if (!resp?.success) {
        toast({ kind: 'error', title: t('common.errorGeneric') });
        return;
      }
      setDialog(null);
      await Promise.all([loadMembers(), loadBlocked()]);
      toast({
        kind: 'warm',
        title: t('settings.block.toast', { name: member.displayName }),
        action: {
          label: t('common.undo'),
          onClick: () => {
            void (async () => {
              await unblockMember(albumId, member.userId).catch(() => null);
              await addMember(albumId, member.userId, member.role).catch(() => null);
              await Promise.all([loadMembers(), loadBlocked()]);
            })();
          },
        },
      });
    },
    [albumId, loadMembers, loadBlocked, t, toast],
  );

  const doUnblock = useCallback(
    async (person: BlockedMemberDto) => {
      const resp = await unblockMember(albumId, person.userId).catch(() => null);
      if (!resp?.success) {
        toast({ kind: 'error', title: t('common.errorGeneric') });
        return;
      }
      await loadBlocked();
      toast({
        kind: 'success',
        title: t('settings.blocked.unblocked', { name: person.displayName }),
      });
    },
    [albumId, loadBlocked, t, toast],
  );

  const doRevoke = useCallback(
    async (token: string) => {
      setDialogBusy(true);
      const resp = await revokeInvite(albumId, token).catch(() => null);
      setDialogBusy(false);
      if (!resp?.success) {
        toast({ kind: 'error', title: t('common.errorGeneric') });
        return;
      }
      setDialog(null);
      await loadInvites();
      toast({ kind: 'success', title: t('settings.invites.revoked') });
    },
    [albumId, loadInvites, t, toast],
  );

  const doDelete = useCallback(async () => {
    if (!album) return;
    setDialogBusy(true);
    const resp = await deleteAlbum(albumId).catch(() => null);
    setDialogBusy(false);
    if (!resp?.success) {
      toast({ kind: 'error', title: t('common.errorGeneric') });
      return;
    }
    setDialog(null);
    void navigate({ to: '/trips', search: {} });
    toast({ kind: 'success', title: t('settings.delete.toast', { title: album.title }) });
  }, [album, albumId, navigate, t, toast]);

  const doLeave = useCallback(async () => {
    if (!album || !user) return;
    setDialogBusy(true);
    const resp = await removeMember(albumId, user.id).catch(() => null);
    setDialogBusy(false);
    if (!resp?.success) {
      toast({ kind: 'error', title: t('common.errorGeneric') });
      return;
    }
    setDialog(null);
    void navigate({ to: '/trips', search: {} });
    toast({ kind: 'success', title: t('settings.leave.toast', { title: album.title }) });
  }, [album, albumId, user, navigate, t, toast]);

  // ── Mobile context bar ──
  const contextBar = useMemo(
    () =>
      isMobile && album ? (
        <MobileContextBar
          title={`${album.title} · ${t('settings.title')}`}
          onBack={() => void navigate({ to: '/trips/$albumId', params: { albumId } })}
        />
      ) : null,
    [isMobile, album, albumId, navigate, t],
  );
  useMobileContextBar(contextBar);

  // ── Album unavailable ──
  if (albumError === 'forbidden') {
    return (
      <div className={`${styles.page} page-enter`}>
        <div className={styles.notFound}>
          <EmptyState
            title={t('trip.notAvailable.title')}
            body={t('trip.notAvailable.body')}
            actions={
              <Button as="a" href="/trips" variant="secondary">
                {t('trip.backToTrips')}
              </Button>
            }
          />
        </div>
      </div>
    );
  }
  if (albumError === 'error') {
    return (
      <div className={`${styles.page} page-enter`}>
        <div className={styles.notFound}>
          <EmptyState
            kind="error"
            title={t('common.connectionError')}
            actions={<Button onClick={() => void loadAlbum()}>{t('common.retry')}</Button>}
          />
        </div>
      </div>
    );
  }

  const selfId = user?.id ?? '';

  return (
    <div className={`${styles.page} page-enter`}>
      <div className={styles.crumbs}>
        <button
          type="button"
          className={styles.crumbBack}
          aria-label={t('nav.back')}
          onClick={() => void navigate({ to: '/trips/$albumId', params: { albumId } })}
        >
          ‹
        </button>
        {album ? (
          <span className={styles.crumbTitle}>{album.title}</span>
        ) : (
          <Skeleton width={120} height={14} />
        )}
        <span className={styles.crumbSep} aria-hidden="true">
          /
        </span>
        <span className={styles.crumbHere}>{t('settings.title')}</span>
      </div>

      <div className={styles.layout}>
        <nav className={styles.rail} aria-label={t('settings.sections')}>
          <span className={`t-label ${styles.railHeading}`}>{t('settings.sections')}</span>
          {rail.map((entry) => (
            <button
              key={entry.id}
              type="button"
              className={[styles.railItem, entry.danger ? styles.railDanger : '']
                .filter(Boolean)
                .join(' ')}
              aria-current={active === entry.id ? 'true' : undefined}
              onClick={() => goToSection(entry.id)}
            >
              {entry.label}
              {entry.count !== undefined && (
                <span className={styles.railCount}>{entry.count}</span>
              )}
            </button>
          ))}
        </nav>

        <div className={styles.content}>
          {roleKnown && !isOwner && (
            <p className={`t-body-s ${styles.readOnlyNote}`}>{t('settings.readOnly.note')}</p>
          )}

          {/* ── 1 · Details ── */}
          <section id="details" className={styles.section} aria-labelledby="details-h">
            <h2 className="t-display-s" id="details-h">
              {t('settings.details')}
            </h2>
            {!album || !form ? (
              <Delayed>
                <div className={styles.detailsGrid}>
                  <Skeleton height={62} radius="var(--r-sm)" />
                  <Skeleton height={62} radius="var(--r-sm)" />
                </div>
              </Delayed>
            ) : isOwner ? (
              <form
                className={styles.sectionBody}
                onSubmit={(e) => {
                  e.preventDefault();
                  void saveDetails();
                }}
              >
                <div className={styles.detailsGrid}>
                  <Input
                    label={t('settings.details.name')}
                    value={form.title}
                    maxLength={80}
                    error={nameError}
                    onChange={(e) => {
                      setForm({ ...form, title: e.target.value });
                      if (nameError) setNameError(null);
                    }}
                  />
                  <Textarea
                    label={t('settings.details.description')}
                    optionalTag
                    showCounter
                    rows={2}
                    maxLength={300}
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                  />
                </div>
                <div className={styles.saveRow}>
                  <span className={styles.saveDesktop}>
                    <Button
                      type="submit"
                      size="sm"
                      disabled={!dirty}
                      loading={saving}
                      loadingLabel={t('common.saving')}
                    >
                      {t('common.saveChanges')}
                    </Button>
                  </span>
                  <span className="t-meta">{t('settings.details.coverNote')}</span>
                </div>
              </form>
            ) : (
              <div className={styles.sectionBody}>
                <div className={styles.detailsGrid}>
                  <div className={styles.readOnlyField}>
                    <span className={`t-label ${styles.readOnlyLabel}`}>
                      {t('settings.details.name')}
                    </span>
                    <span className={styles.readOnlyValue}>{album.title}</span>
                  </div>
                  <div className={styles.readOnlyField}>
                    <span className={`t-label ${styles.readOnlyLabel}`}>
                      {t('settings.details.description')}
                    </span>
                    <span className={styles.readOnlyValue}>
                      {album.description?.trim() ? album.description : '—'}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </section>

          {/* ── 2 · Privacy and sharing ── */}
          {isOwner && (
            <section id="privacy" className={styles.section} aria-labelledby="privacy-h">
              <h2 className="t-display-s" id="privacy-h">
                {t('settings.privacy')}
              </h2>
              {!album ? (
                <Delayed>
                  <div className={styles.sectionBody}>
                    <Skeleton height={72} radius="var(--r-md)" />
                    <Skeleton height={72} radius="var(--r-md)" />
                  </div>
                </Delayed>
              ) : (
                <div className={styles.sectionBody}>
                  <div className={styles.radioCards}>
                    <div
                      className={[
                        styles.radioCard,
                        album.visibility === 'PRIVATE' ? styles.radioCardOn : '',
                      ]
                        .filter(Boolean)
                        .join(' ')}
                    >
                      <label className={styles.radioMain}>
                        <input
                          type="radio"
                          name="visibility"
                          className={styles.radio}
                          checked={album.visibility === 'PRIVATE'}
                          disabled={visibilityBusy}
                          onChange={() => setDialog({ kind: 'privacy' })}
                        />
                        <span>
                          <span className={styles.radioTitle}>{t('settings.privacy.private')}</span>
                          <span className={`t-body-s ${styles.radioBody}`}>
                            {t('settings.privacy.privateBody', { n: memberCount })}
                          </span>
                        </span>
                      </label>
                    </div>

                    <div
                      className={[
                        styles.radioCard,
                        album.visibility === 'LINK_SHARED' ? styles.radioCardOn : '',
                      ]
                        .filter(Boolean)
                        .join(' ')}
                    >
                      <label className={styles.radioMain}>
                        <input
                          type="radio"
                          name="visibility"
                          className={styles.radio}
                          checked={album.visibility === 'LINK_SHARED'}
                          disabled={visibilityBusy}
                          onChange={() => void applyVisibility('LINK_SHARED')}
                        />
                        <span>
                          <span className={styles.radioTitle}>{t('settings.privacy.link')}</span>
                          <span className={`t-body-s ${styles.radioBody}`}>
                            {t('settings.privacy.linkBody')}
                          </span>
                        </span>
                      </label>
                      {album.visibility === 'LINK_SHARED' && (
                        <div className={styles.urlRow}>
                          <span className={styles.mono}>{shareUrl}</span>
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() =>
                              void copyText(
                                shareUrl,
                                t('trip.share.copied'),
                                t('trip.share.copiedBody', { title: album.title }),
                              )
                            }
                          >
                            {t('common.copy')}
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </section>
          )}

          {/* ── 3 · People ── */}
          <section id="people" className={styles.section} aria-labelledby="people-h">
            <div className={styles.sectionHead}>
              <h2 className="t-display-s" id="people-h">
                {t('settings.people')}{' '}
                {members && <span className={styles.headCount}>{members.length}</span>}
              </h2>
              {isOwner && (
                <Button variant="warm" size="sm" onClick={() => goToSection('invites')}>
                  {t('trip.inviteFriends')}
                </Button>
              )}
            </div>
            {!members ? (
              <Delayed>
                <div className={styles.sectionBody}>
                  <ListSkeleton rows={3} />
                </div>
              </Delayed>
            ) : (
              <ul className={styles.rows}>
                {members.map((member) => {
                  const uploads = uploadCounts.get(member.userId) ?? 0;
                  const isSelf = member.userId === selfId;
                  const canAct = isOwner && !isSelf && member.role !== 'OWNER';
                  return (
                    <li key={member.userId} className={styles.row}>
                      <Avatar
                        userId={member.userId}
                        displayName={member.displayName}
                        avatarUrl={member.avatarUrl}
                        size="md"
                      />
                      <span className={styles.rowMain}>
                        <span className={styles.rowName}>
                          {member.displayName}
                          {isSelf && <span className={styles.rowYou}> · {t('common.you')}</span>}
                        </span>
                        <span className="t-meta">
                          {uploads > 0
                            ? t('settings.people.joinedWithCount', {
                                date: formatDateMedium(member.joinedAt, lang),
                                n: uploads,
                              })
                            : t('settings.people.joined', {
                                date: formatDateMedium(member.joinedAt, lang),
                              })}
                        </span>
                      </span>
                      <Badge kind={ROLE_BADGE[member.role]}>
                        {member.role === 'OWNER'
                          ? t('role.owner')
                          : member.role === 'CONTRIBUTOR'
                            ? t('role.contributor')
                            : t('role.viewer')}
                      </Badge>
                      <span className={styles.rowEnd}>
                        {canAct && (
                          <Menu
                            label={t('settings.people.memberActions', { name: member.displayName })}
                            trigger={(props) => (
                              <button
                                type="button"
                                className={styles.overflow}
                                aria-label={t('settings.people.memberActions', {
                                  name: member.displayName,
                                })}
                                {...props}
                              >
                                <MoreHorizontal size={16} aria-hidden="true" />
                              </button>
                            )}
                            items={[
                              {
                                label: t('settings.people.remove'),
                                danger: true,
                                onSelect: () => setDialog({ kind: 'remove', member }),
                              },
                              {
                                label: t('settings.people.block'),
                                danger: true,
                                onSelect: () => setDialog({ kind: 'block', member }),
                              },
                            ]}
                          />
                        )}
                      </span>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          {/* ── 4 · Invites ── */}
          {isOwner && (
            <section id="invites" className={styles.section} aria-labelledby="invites-h">
              <h2 className="t-display-s" id="invites-h">
                {t('settings.invites')}
              </h2>
              {!invites ? (
                <Delayed>
                  <div className={styles.sectionBody}>
                    <Skeleton height={68} radius="var(--r-md)" />
                  </div>
                </Delayed>
              ) : (
                <div className={styles.sectionBody}>
                  {invites.length === 0 ? (
                    <p className={`t-body-s ${styles.muted}`}>{t('settings.invites.none')}</p>
                  ) : (
                    <ul className={styles.inviteList}>
                      {invites.map((invite) => (
                        <li key={invite.token} className={styles.inviteCard}>
                          <span className={styles.inviteMain}>
                            <span className={styles.mono}>
                              {`${window.location.origin}/invite/${invite.token}`}
                            </span>
                            <span className="t-meta">
                              {t('settings.invites.expires', {
                                date: formatDateMedium(invite.expiresAt, lang),
                              })}
                              {invite.joinCount > 0 &&
                                ` · ${
                                  invite.joinCount === 1
                                    ? t('settings.invites.joined.one')
                                    : t('settings.invites.joined', { n: invite.joinCount })
                                }`}
                            </span>
                          </span>
                          <span className={styles.inviteActions}>
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={() =>
                                void copyText(
                                  `${window.location.origin}/invite/${invite.token}`,
                                  t('settings.invites.copied'),
                                )
                              }
                            >
                              {t('settings.invites.copy')}
                            </Button>
                            <Button
                              variant="secondary"
                              size="sm"
                              loading={busyToken === invite.token}
                              onClick={() => void regenerate(invite.token)}
                            >
                              {t('settings.invites.regenerate')}
                            </Button>
                            <Button
                              variant="danger-quiet"
                              size="sm"
                              onClick={() => setDialog({ kind: 'revoke', token: invite.token })}
                            >
                              {t('settings.invites.revoke')}
                            </Button>
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                  <div className={styles.inviteForm}>
                    <Select
                      label={t('settings.invites.expiresIn')}
                      value={expiresIn}
                      onChange={(e) => setExpiresIn(e.target.value)}
                    >
                      <option value="1">{t('settings.invites.days.1')}</option>
                      <option value="7">{t('settings.invites.days.7')}</option>
                      <option value="30">{t('settings.invites.days.30')}</option>
                      <option value="90">{t('settings.invites.days.90')}</option>
                    </Select>
                    <Button
                      variant="secondary"
                      loading={creating}
                      loadingLabel={t('settings.invites.creating')}
                      onClick={() => void createLink()}
                    >
                      {t('settings.invites.create')}
                    </Button>
                  </div>
                </div>
              )}
            </section>
          )}

          {/* ── 5 · Blocked — absent entirely when nobody is blocked ── */}
          {isOwner && blocked && blocked.length > 0 && (
            <section id="blocked" className={styles.section} aria-labelledby="blocked-h">
              <h2 className="t-display-s" id="blocked-h">
                {t('settings.blocked')} <span className={styles.headCount}>{blocked.length}</span>
              </h2>
              <ul className={styles.rows}>
                {blocked.map((person) => (
                  <li key={person.userId} className={styles.row}>
                    <Avatar
                      userId={person.userId}
                      displayName={person.displayName}
                      avatarUrl={person.avatarUrl}
                      size="md"
                    />
                    <span className={styles.rowMain}>
                      <span className={styles.rowName}>{person.displayName}</span>
                      <span className="t-meta">
                        {t('settings.blocked.at', {
                          date: formatDateMedium(person.blockedAt, lang),
                        })}
                      </span>
                    </span>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => void doUnblock(person)}
                    >
                      {t('settings.blocked.unblock')}
                    </Button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* ── 6 · Danger zone / Leave ── */}
          {isOwner && (
            <section
              id="danger"
              className={`${styles.section} ${styles.sectionDanger}`}
              aria-labelledby="danger-h"
            >
              <h2 className={`t-display-s ${styles.dangerHeading}`} id="danger-h">
                {t('settings.danger')}
              </h2>
              {!album || media === null ? (
                <Delayed>
                  <div className={styles.sectionBody}>
                    <Skeleton height={84} radius="var(--r-md)" />
                  </div>
                </Delayed>
              ) : (
                <div className={styles.dangerCard}>
                  <span className={styles.dangerText}>
                    <span className={styles.dangerTitle}>{t('settings.danger.title')}</span>
                    <span className={`t-body-s ${styles.dangerBody}`}>
                      {t('settings.danger.body', { p: photoCount, m: memberCount })}
                    </span>
                  </span>
                  <Button variant="danger" size="sm" onClick={() => setDialog({ kind: 'delete' })}>
                    {t('settings.danger.cta')}
                  </Button>
                </div>
              )}
            </section>
          )}

          {roleKnown && !isOwner && (
            <section
              id="leave"
              className={`${styles.section} ${styles.sectionDanger}`}
              aria-labelledby="leave-h"
            >
              <h2 className={`t-display-s ${styles.dangerHeading}`} id="leave-h">
                {t('trip.leave')}
              </h2>
              <div className={styles.dangerCard}>
                <span className={styles.dangerText}>
                  <span className={`t-body-s ${styles.dangerBody}`}>{t('settings.leave.body')}</span>
                </span>
                <Button variant="danger" size="sm" onClick={() => setDialog({ kind: 'leave' })}>
                  {t('settings.leave.cta')}
                </Button>
              </div>
            </section>
          )}
        </div>
      </div>

      {/* Sticky save bar — mobile only, and only while there is something to save. */}
      {isOwner && isMobile && dirty && (
        <div className={styles.saveBar}>
          <Button
            loading={saving}
            loadingLabel={t('common.saving')}
            onClick={() => void saveDetails()}
          >
            {t('common.saveChanges')}
          </Button>
        </div>
      )}

      {renderDialog()}
    </div>
  );

  function renderDialog(): ReactNode {
    if (!dialog || !album) return null;
    switch (dialog.kind) {
      case 'privacy':
        return (
          <Confirm
            open
            danger={false}
            busy={visibilityBusy}
            onClose={closeDialog}
            onConfirm={async () => {
              const ok = await applyVisibility('PRIVATE');
              if (ok) setDialog(null);
            }}
            title={t('settings.privacy.confirmTitle', { title: album.title })}
            body={t('settings.privacy.confirmBody')}
            confirmLabel={t('settings.privacy.confirmCta')}
            cancelLabel={t('common.cancel')}
          />
        );
      case 'remove': {
        const uploads = uploadCounts.get(dialog.member.userId) ?? 0;
        return (
          <Confirm
            open
            busy={dialogBusy}
            onClose={closeDialog}
            onConfirm={() => doRemove(dialog.member)}
            title={t('settings.remove.title', {
              name: dialog.member.displayName,
              title: album.title,
            })}
            body={
              uploads > 0
                ? t('settings.remove.body', { n: uploads })
                : t('settings.remove.bodyNoPhotos')
            }
            confirmLabel={t('settings.remove.cta', { name: dialog.member.displayName })}
            cancelLabel={t('settings.remove.keep')}
          />
        );
      }
      case 'block':
        return (
          <Confirm
            open
            busy={dialogBusy}
            onClose={closeDialog}
            onConfirm={() => doBlock(dialog.member)}
            title={t('settings.block.title', {
              name: dialog.member.displayName,
              title: album.title,
            })}
            body={t('settings.block.body')}
            confirmLabel={t('settings.block.cta', { name: dialog.member.displayName })}
            cancelLabel={t('common.cancel')}
          />
        );
      case 'revoke':
        return (
          <Confirm
            open
            busy={dialogBusy}
            onClose={closeDialog}
            onConfirm={() => doRevoke(dialog.token)}
            title={t('settings.invites.revokeTitle')}
            body={t('settings.invites.revokeBody')}
            confirmLabel={t('settings.invites.revoke')}
            cancelLabel={t('common.cancel')}
          />
        );
      case 'delete':
        return (
          <Confirm
            open
            busy={dialogBusy}
            onClose={closeDialog}
            onConfirm={doDelete}
            title={t('settings.delete.title', { title: album.title })}
            body={t('settings.delete.body')}
            confirmLabel={t('settings.danger.cta')}
            cancelLabel={t('common.cancel')}
            typeToConfirm={album.title}
            typeToConfirmPlaceholder={t('settings.delete.placeholder', { title: album.title })}
          />
        );
      case 'leave':
        return (
          <Confirm
            open
            busy={dialogBusy}
            onClose={closeDialog}
            onConfirm={doLeave}
            title={t('settings.leave.title', { title: album.title })}
            body={t('settings.leave.body')}
            confirmLabel={t('settings.leave.cta')}
            cancelLabel={t('common.cancel')}
          />
        );
    }
  }
}
