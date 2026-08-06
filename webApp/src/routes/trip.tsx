import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { Link, useNavigate, useParams, useSearch } from '@tanstack/react-router';
import { MoreHorizontal, Upload } from 'lucide-react';
import { getAlbum, getMembers, removeMember, updateAlbum, type AlbumDto, type AlbumMemberDto } from '../api/albums';
import { getAlbumMedia, type MediaDto } from '../api/media';
import { getAlbumActivity, type ActivityEventDto } from '../api/activity';
import { getReactions, REACTION_EMOJI } from '../api/reactions';
import { getComments } from '../api/comments';
import { useAuth } from '../auth/AuthContext';
import { useLocale } from '../i18n/LocaleProvider';
import { useMobileContextBar, MobileContextBar } from '../app/AppShell';
import { deriveTripActivity, type DerivedActivityEvent } from '../lib/activityDerive';
import { chapterOf, groupIntoChapters } from '../lib/chapters';
import {
  formatDateTime,
  formatMonthYear,
  relativeTimeParts,
} from '../lib/format';
import { markVisited, markWelcomed, wasWelcomed } from '../lib/lastVisit';
import { Avatar, AvatarStack } from '../ui/Avatar';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { Lightbox } from '../ui/Lightbox';
import { MediaGallery, type GalleryView } from '../ui/MediaGallery';
import { Menu } from '../ui/Menu';
import { Modal, ModalBody, Confirm } from '../ui/Modal';
import { Plate } from '../ui/Plate';
import { Segmented, Tabs, tabClass } from '../ui/Tabs';
import { Skeleton, Delayed } from '../ui/Skeleton';
import { useToast } from '../ui/Toast';
import { useDocumentTitle, useMediaQuery } from '../ui/hooks';
import { DropOverlay } from '../upload/UploadDock';
import { useUploads } from '../upload/UploadProvider';
import styles from './trip.module.css';

type MediaFilter = 'all' | 'photos' | 'videos';

// Session cache of per-media reaction/comment counts, filled opportunistically
// (lightbox visits, strip fetches) — powers the hover chips without N×2
// requests for a 300-photo trip.
const countsCache = new Map<string, { reactions: number; comments: number }>();

export function TripPage() {
  const { albumId } = useParams({ strict: false }) as { albumId: string };
  const search = useSearch({ strict: false }) as { m?: string; joined?: string };
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t, lang } = useLocale();
  const { toast } = useToast();
  const uploads = useUploads();
  const isMobile = useMediaQuery('(max-width: 767px)');

  const [album, setAlbum] = useState<AlbumDto | null>(null);
  const [albumError, setAlbumError] = useState<'forbidden' | 'error' | null>(null);
  const [members, setMembers] = useState<AlbumMemberDto[] | null>(null);
  const [media, setMedia] = useState<MediaDto[] | null>(null);
  const [mediaError, setMediaError] = useState<string | null>(null);
  const [serverActivity, setServerActivity] = useState<ActivityEventDto[] | null>(null);

  const [view, setView] = useState<GalleryView>(() => {
    try {
      return localStorage.getItem(`thykra.view.${albumId}`) === 'contact' ? 'contact' : 'chapters';
    } catch {
      return 'chapters';
    }
  });
  const [filter, setFilter] = useState<MediaFilter>('all');
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [shareOpen, setShareOpen] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);
  const [leaveBusy, setLeaveBusy] = useState(false);
  const [welcome, setWelcome] = useState(false);
  const [dragOver, setDragOver] = useState(0);
  const [countsVersion, setCountsVersion] = useState(0);

  const titleRef = useRef<HTMLHeadingElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const plateRefs = useRef(new Map<string, HTMLButtonElement>());
  const originRectRef = useRef<DOMRect | null>(null);

  const role = useMemo(() => {
    if (!members || !user) return null;
    return members.find((m) => m.userId === user.id)?.role ?? null;
  }, [members, user]);
  const isOwner = role === 'OWNER';
  const canUpload = role === 'OWNER' || role === 'CONTRIBUTOR';

  useDocumentTitle(album ? `${album.title} · Thykra` : null);

  // ── Fetch ──
  const loadAlbum = useCallback(async () => {
    setAlbumError(null);
    const resp = await getAlbum(albumId).catch(() => null);
    if (resp?.success && resp.data) setAlbum(resp.data);
    else if (resp && !resp.success) setAlbumError('forbidden');
    else setAlbumError('error');
  }, [albumId]);

  const loadMedia = useCallback(async () => {
    setMediaError(null);
    const resp = await getAlbumMedia(albumId).catch(() => null);
    if (resp?.success && resp.data) setMedia(resp.data);
    else setMediaError(resp?.error ?? t('common.connectionError'));
  }, [albumId, t]);

  useEffect(() => {
    void loadAlbum();
    void loadMedia();
    getMembers(albumId).then((resp) => {
      if (resp.success && resp.data) setMembers(resp.data);
    });
    getAlbumActivity(albumId).then(
      (resp) => {
        if (resp.success && resp.data) setServerActivity(resp.data.items);
      },
      () => undefined,
    );
    markVisited(albumId);
  }, [albumId, loadAlbum, loadMedia]);

  // ── J2 handoff: welcome banner, once per trip ──
  useEffect(() => {
    if (search.joined === '1') {
      if (!wasWelcomed(albumId)) {
        setWelcome(true);
        markWelcomed(albumId);
      }
      void navigate({
        to: '/trips/$albumId',
        params: { albumId },
        search: (prev: Record<string, unknown>) => ({ ...prev, joined: undefined }),
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [albumId, search.joined]);

  // Focus the h1 when arriving from trip creation.
  useEffect(() => {
    if (album && window.history.state?.focusTitle) {
      titleRef.current?.focus();
    }
  }, [album]);

  // ── Uploads: confirmed media insert into their chapter; batch toasts ──
  useEffect(() => {
    const offConfirmed = uploads.onMediaConfirmed((m) => {
      if (m.albumId !== albumId) return;
      setMedia((prev) => (prev && !prev.some((x) => x.id === m.id) ? [...prev, m] : prev));
    });
    const offSettled = uploads.onBatchSettled((summary) => {
      if (summary.albumId !== albumId) return;
      // Refetch the album — the cover resolves from the latest upload
      // (the product's first celebration when it's the first photo).
      void loadAlbum();
      if (summary.added.length > 0) {
        const chapters = groupIntoChapters([...(media ?? []), ...summary.added]);
        const target = chapterOf(chapters, summary.added[0].id);
        toast({
          kind: summary.failed > 0 ? 'warm' : 'success',
          title:
            summary.failed > 0
              ? t('upload.partial', { failed: summary.failed, total: summary.total })
              : t('upload.done.count', { n: summary.added.length }),
          body:
            target?.ordinal != null
              ? t('upload.done.dest', { trip: summary.albumTitle, day: target.ordinal })
              : summary.albumTitle,
          action:
            target != null
              ? {
                  label: t('common.open'),
                  onClick: () => scrollToChapter(target.key),
                }
              : undefined,
        });
      }
    });
    return () => {
      offConfirmed();
      offSettled();
    };
  }, [albumId, loadAlbum, media, t, toast, uploads]);

  // ── Deep link ?m= ──
  const sortedMedia = useMemo(() => {
    if (!media) return null;
    const chapters = groupIntoChapters(media);
    return chapters.flatMap((c) => c.items);
  }, [media]);

  const filteredMedia = useMemo(() => {
    if (!sortedMedia) return null;
    if (filter === 'photos') return sortedMedia.filter((m) => m.type === 'PHOTO');
    if (filter === 'videos') return sortedMedia.filter((m) => m.type === 'VIDEO');
    return sortedMedia;
  }, [sortedMedia, filter]);

  useEffect(() => {
    if (!search.m || !sortedMedia || lightboxIndex !== null) return;
    const idx = sortedMedia.findIndex((m) => m.id === search.m);
    if (idx >= 0) {
      setLightboxIndex(idx);
    } else {
      toast({ kind: 'info', title: t('lightbox.mediaNotFound') });
      void navigate({
        to: '/trips/$albumId',
        params: { albumId },
        search: {},
        replace: true,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search.m, sortedMedia]);

  const openLightbox = useCallback(
    (item: MediaDto) => {
      if (!sortedMedia) return;
      const idx = sortedMedia.findIndex((m) => m.id === item.id);
      if (idx < 0) return;
      originRectRef.current =
        plateRefs.current.get(item.id)?.getBoundingClientRect() ?? null;
      setLightboxIndex(idx);
      void navigate({
        to: '/trips/$albumId',
        params: { albumId },
        search: { m: item.id },
        replace: true,
      });
    },
    [albumId, navigate, sortedMedia],
  );

  const closeLightbox = useCallback(() => {
    const current = lightboxIndex !== null ? sortedMedia?.[lightboxIndex] : null;
    setLightboxIndex(null);
    void navigate({
      to: '/trips/$albumId',
      params: { albumId },
      search: {},
      replace: true,
    });
    // Focus returns to the exact plate.
    if (current) {
      requestAnimationFrame(() => plateRefs.current.get(current.id)?.focus());
    }
  }, [albumId, lightboxIndex, navigate, sortedMedia]);

  const onLightboxIndex = useCallback(
    (idx: number) => {
      setLightboxIndex(idx);
      const m = sortedMedia?.[idx];
      if (m) {
        // Cache counts opportunistically for the grid chips.
        void getReactions(m.albumId, m.id).then((resp) => {
          if (resp.success && resp.data) {
            const prev = countsCache.get(m.id);
            countsCache.set(m.id, {
              reactions: resp.data.summary.reduce((sum, s) => sum + s.count, 0),
              comments: prev?.comments ?? 0,
            });
            setCountsVersion((v) => v + 1);
          }
        });
        void getComments(m.albumId, m.id).then((resp) => {
          if (resp.success && resp.data) {
            const prev = countsCache.get(m.id);
            countsCache.set(m.id, {
              reactions: prev?.reactions ?? 0,
              comments: resp.data.length,
            });
            setCountsVersion((v) => v + 1);
          }
        });
        void navigate({
          to: '/trips/$albumId',
          params: { albumId },
          search: { m: m.id },
          replace: true,
        });
      }
    },
    [albumId, navigate, sortedMedia],
  );

  // ── Whole-page drop target ──
  useEffect(() => {
    if (!canUpload) return;
    const onDragEnter = (e: DragEvent) => {
      if (e.dataTransfer?.types.includes('Files')) {
        e.preventDefault();
        setDragOver((d) => d + 1);
      }
    };
    const onDragOver = (e: DragEvent) => {
      if (e.dataTransfer?.types.includes('Files')) e.preventDefault();
    };
    const onDragLeave = (e: DragEvent) => {
      if (e.dataTransfer?.types.includes('Files')) setDragOver((d) => Math.max(0, d - 1));
    };
    const onDrop = (e: DragEvent) => {
      if (!e.dataTransfer?.files.length) return;
      e.preventDefault();
      setDragOver(0);
      handleFiles([...e.dataTransfer.files]);
    };
    window.addEventListener('dragenter', onDragEnter);
    window.addEventListener('dragover', onDragOver);
    window.addEventListener('dragleave', onDragLeave);
    window.addEventListener('drop', onDrop);
    return () => {
      window.removeEventListener('dragenter', onDragEnter);
      window.removeEventListener('dragover', onDragOver);
      window.removeEventListener('dragleave', onDragLeave);
      window.removeEventListener('drop', onDrop);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canUpload, album]);

  const handleFiles = (files: File[]) => {
    if (!album) return;
    const rejected = uploads.enqueue(files, albumId, album.title);
    for (const file of rejected) {
      toast({ kind: 'error', title: t('upload.notMedia', { file: file.name }) });
    }
  };

  const scrollToChapter = (key: string) => {
    document
      .querySelector(`[data-chapter-key="${CSS.escape(key)}"]`)
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  // ── Share ──
  const publicUrl = `${window.location.origin}/s/${albumId}`;
  const copyShare = async () => {
    try {
      await navigator.clipboard.writeText(publicUrl);
      toast({
        kind: 'success',
        title: t('trip.share.copied'),
        body: t('trip.share.copiedBody', { title: album?.title ?? '' }),
      });
    } catch {
      toast({ kind: 'error', title: t('common.errorGeneric') });
    }
  };
  const handleShare = () => {
    if (isOwner) setShareOpen(true);
    else if (album?.visibility === 'LINK_SHARED') void copyShare();
  };

  const leaveTrip = async () => {
    if (!user) return;
    setLeaveBusy(true);
    const resp = await removeMember(albumId, user.id).catch(() => null);
    setLeaveBusy(false);
    if (resp?.success) {
      toast({ kind: 'info', title: t('settings.leave.toast', { title: album?.title ?? '' }) });
      void navigate({ to: '/trips', search: {} });
    }
  };

  // ── Activity strip events ──
  const stripEvents = useMemo<(ActivityEventDto | DerivedActivityEvent)[]>(() => {
    if (serverActivity) return serverActivity.slice(0, 4);
    if (media && members) return deriveTripActivity(media, members).slice(0, 4);
    return [];
  }, [serverActivity, media, members]);

  // ── Mobile context bar ──
  const overflowItems = useMemo(() => {
    const items: ({ label: string; onSelect: () => void; danger?: boolean } | 'separator')[] = [];
    if (isOwner || album?.visibility === 'LINK_SHARED') {
      items.push({ label: t('common.share'), onSelect: handleShare });
    }
    if (isOwner) {
      items.push({
        label: t('trip.manage'),
        onSelect: () =>
          void navigate({ to: '/trips/$albumId/settings', params: { albumId } }),
      });
    }
    if (!isOwner && role) {
      items.push('separator');
      items.push({ label: t('trip.leave'), onSelect: () => setLeaveOpen(true), danger: true });
    }
    return items;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOwner, role, album?.visibility, t]);

  useMobileContextBar(
    isMobile && album ? (
      <MobileContextBar
        title={album.title}
        onBack={() => void navigate({ to: '/trips', search: {} })}
        end={
          <Menu
            label={t('nav.menu')}
            items={overflowItems}
            trigger={(props) => (
              <button type="button" className={undefined} aria-label={t('nav.menu')} {...props} style={{ width: 40, height: 40, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--r-sm)' }}>
                <MoreHorizontal size={20} aria-hidden="true" />
              </button>
            )}
          />
        }
      />
    ) : null,
  );

  // ── Not available ──
  if (albumError === 'forbidden') {
    return (
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
    );
  }
  if (albumError === 'error') {
    return (
      <div className={styles.notFound}>
        <EmptyState
          kind="error"
          title={t('common.connectionError')}
          actions={
            <Button onClick={() => void loadAlbum()}>{t('common.retry')}</Button>
          }
        />
      </div>
    );
  }

  const chapters = media ? groupIntoChapters(media) : [];
  const dateRange = (() => {
    if (!media || media.length === 0 || !album) return null;
    const dayCount = chapters.filter((c) => c.dated).length;
    const first = media.reduce(
      (min, m) => Math.min(min, Date.parse(m.takenAt ?? m.uploadedAt)),
      Infinity,
    );
    const label = formatMonthYear(new Date(first).toISOString(), lang);
    return dayCount > 1 ? t('trip.meta.days', { date: label, n: dayCount }) : label;
  })();

  const photoCount = media?.filter((m) => m.type === 'PHOTO').length ?? 0;
  const videoCount = media?.filter((m) => m.type === 'VIDEO').length ?? 0;

  return (
    <div className={`page-enter ${styles.page}`}>
      {dragOver > 0 && album && <DropOverlay albumTitle={album.title} />}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*,video/*"
        multiple
        className="sr-only"
        onChange={(e) => {
          if (e.target.files?.length) handleFiles([...e.target.files]);
          e.target.value = '';
        }}
        tabIndex={-1}
        aria-hidden="true"
      />

      {/* ── Masthead ── */}
      <div className={styles.masthead}>
        <div>
          <div className={styles.microRow}>
            {dateRange && <span className="t-label">{dateRange}</span>}
            {album?.visibility === 'LINK_SHARED' && (
              <Badge kind="ink">{t('trips.card.linkShared')}</Badge>
            )}
          </div>
          {album ? (
            <h1 ref={titleRef} tabIndex={-1} className={`t-display-xl ${styles.title}`}>
              {album.title}
            </h1>
          ) : (
            <Delayed>
              <Skeleton width="60%" height={56} radius={6} />
            </Delayed>
          )}
          {album?.description && (
            <p className={`t-body-l ${styles.description}`}>{album.description}</p>
          )}
          <div className={styles.mastMeta}>
            {album && members && (
              <span className={styles.peopleGroup}>
                <AvatarStack
                  people={album.previewMembers}
                  totalCount={album.memberCount}
                  size="md"
                  onClick={() =>
                    void navigate({
                      to: '/trips/$albumId/settings',
                      params: { albumId },
                      hash: 'people',
                    })
                  }
                />
                <span className="t-body-s">{t('common.people.count', { n: album.memberCount })}</span>
              </span>
            )}
            {media && media.length > 0 && (
              <span className="t-body-s">
                {[
                  photoCount > 0
                    ? photoCount === 1
                      ? t('common.photo.one')
                      : t('common.photos.count', { n: photoCount })
                    : null,
                  videoCount > 0
                    ? videoCount === 1
                      ? t('common.video.one')
                      : t('common.videos.count', { n: videoCount })
                    : null,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
            )}
          </div>
          <div className={styles.actions}>
            {(isOwner || album?.visibility === 'LINK_SHARED') && (
              <Button variant="secondary" size="sm" onClick={handleShare}>
                {t('common.share')}
              </Button>
            )}
            {isOwner && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() =>
                  void navigate({ to: '/trips/$albumId/settings', params: { albumId } })
                }
              >
                {t('trip.manage')}
              </Button>
            )}
            {canUpload && (
              <Button variant="warm" size="sm" onClick={() => fileInputRef.current?.click()}>
                {t('trip.addPhotos')}
              </Button>
            )}
          </div>
        </div>
        <div className={styles.coverCell}>
          {album?.coverUrl ? (
            <Plate
              src={album.coverUrl}
              alt=""
              aspectRatio={isMobile ? '16 / 9' : '4 / 3'}
              eager
            />
          ) : album ? null : (
            <Delayed>
              <Skeleton aspectRatio="4 / 3" radius={0} />
            </Delayed>
          )}
        </div>
      </div>

      {/* ── Tabs ── */}
      <div className={styles.tabsRow}>
        <Tabs label={t('nav.menu')}>
          <Link
            to="/trips/$albumId"
            params={{ albumId }}
            search={{}}
            className={tabClass}
            aria-current="page"
          >
            {t('trip.tab.trip')}
          </Link>
          <Link
            to="/trips/$albumId/activity"
            params={{ albumId }}
            className={tabClass}
          >
            {t('trip.tab.activity')}
          </Link>
          <Link to="/trips/$albumId/recaps" params={{ albumId }} className={tabClass}>
            {t('trip.tab.recaps')}
          </Link>
          {isOwner && (
            <Link to="/trips/$albumId/settings" params={{ albumId }} className={tabClass}>
              {t('trip.tab.settings')}
            </Link>
          )}
        </Tabs>
        {media && media.length > 0 && (
          <span className={styles.viewToggle}>
            <Segmented
              label={t('trip.view.chapters')}
              options={[
                { value: 'chapters', label: t('trip.view.chapters') },
                { value: 'contact', label: t('trip.view.contactSheet') },
              ]}
              value={view}
              onChange={(v) => {
                setView(v);
                try {
                  localStorage.setItem(`thykra.view.${albumId}`, v);
                } catch {
                  /* private mode */
                }
              }}
            />
          </span>
        )}
      </div>

      {/* ── Welcome band (J2 handoff) ── */}
      {welcome && canUpload && (
        <div className={styles.welcome} role="status">
          <span className={styles.welcomeText}>
            {t('trip.welcome.before')}
            <span className="accent-phrase">{t('trip.welcome.accent')}</span>
          </span>
          <Button variant="warm" size="sm" onClick={() => fileInputRef.current?.click()}>
            {t('trip.addPhotos')}
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setWelcome(false)}>
            {t('trip.welcome.dismiss')}
          </Button>
        </div>
      )}

      {/* ── Activity strip ── */}
      {stripEvents.length > 0 && (
        <div className={styles.strip}>
          {stripEvents.map((event) => (
            <StripCard
              key={event.id}
              event={event}
              onOpen={(mediaId) => {
                if (!mediaId || !media) return;
                const target = chapterOf(chapters, mediaId);
                if (target) scrollToChapter(target.key);
              }}
            />
          ))}
          <Link
            to="/trips/$albumId/activity"
            params={{ albumId }}
            className={styles.stripMore}
          >
            {t('common.seeAll')}
          </Link>
        </div>
      )}

      {/* ── Gallery ── */}
      <div className={styles.gallerySection}>
        {media && media.length > 0 && videoCount > 0 && (
          <div className={styles.filterRow}>
            <Segmented
              label={t('trip.filter.all', { n: media.length })}
              options={[
                { value: 'all', label: t('trip.filter.all', { n: media.length }) },
                {
                  value: 'photos',
                  label: t('trip.filter.photos', { n: photoCount }),
                  disabled: photoCount === 0,
                },
                {
                  value: 'videos',
                  label: t('trip.filter.videos', { n: videoCount }),
                  disabled: videoCount === 0,
                },
              ]}
              value={filter}
              onChange={setFilter}
            />
          </div>
        )}

        {media === null && !mediaError && (
          <Delayed>
            <div className={styles.loadState} aria-hidden="true">
              <Skeleton aspectRatio="16 / 9" radius={0} />
              <div className={styles.loadRows}>
                <Skeleton style={{ flex: 1.5 }} height={180} radius={0} />
                <Skeleton style={{ flex: 1 }} height={180} radius={0} />
                <Skeleton style={{ flex: 0.75 }} height={180} radius={0} />
              </div>
              <div className={styles.loadRows}>
                <Skeleton style={{ flex: 1 }} height={180} radius={0} />
                <Skeleton style={{ flex: 1.4 }} height={180} radius={0} />
                <Skeleton style={{ flex: 1.1 }} height={180} radius={0} />
              </div>
            </div>
          </Delayed>
        )}

        {mediaError && (
          <EmptyState
            kind="error"
            title={t('trip.media.error.title')}
            body={mediaError}
            actions={<Button onClick={() => void loadMedia()}>{t('common.retry')}</Button>}
          />
        )}

        {media && media.length === 0 && (
          <EmptyState
            title={t('trip.empty.title')}
            body={canUpload ? t('trip.empty.body') : t('trip.empty.viewer')}
            actions={
              canUpload ? (
                <>
                  <Button variant="warm" onClick={() => fileInputRef.current?.click()}>
                    {t('trip.addPhotos')}
                  </Button>
                  {isOwner && (
                    <Button
                      variant="secondary"
                      onClick={() =>
                        void navigate({
                          to: '/trips/$albumId/settings',
                          params: { albumId },
                          hash: 'invites',
                        })
                      }
                    >
                      {t('trip.inviteFriends')}
                    </Button>
                  )}
                </>
              ) : undefined
            }
          />
        )}

        {filteredMedia && filteredMedia.length > 0 && members && (
          <MediaGallery
            media={filteredMedia}
            view={view}
            animKey={albumId}
            stickyTop={isMobile ? 52 + 40 : 56 + 40}
            onOpen={openLightbox}
            registerItem={(id, el) => {
              if (el) plateRefs.current.set(id, el);
              else plateRefs.current.delete(id);
            }}
            labelFor={(item) => {
              void countsVersion;
              const uploader =
                members.find((m) => m.userId === item.uploaderId)?.displayName ?? '';
              const when = formatDateTime(item.takenAt ?? item.uploadedAt, lang);
              const counts = countsCache.get(item.id);
              const parts = [
                `${item.type === 'VIDEO' ? t('media.video') : t('media.photo')} — ${uploader}, ${when}`,
              ];
              if (counts?.reactions) parts.push(t('media.reactions.count', { n: counts.reactions }));
              if (counts?.comments) parts.push(t('media.comments.count', { n: counts.comments }));
              return parts.join(', ');
            }}
            chipsFor={(item) => {
              void countsVersion;
              const counts = countsCache.get(item.id);
              if (!counts) return undefined;
              const chips: string[] = [];
              if (counts.reactions > 0) chips.push(`❤️ ${counts.reactions}`);
              if (counts.comments > 0) chips.push(`💬 ${counts.comments}`);
              return chips.length ? chips : undefined;
            }}
          />
        )}
      </div>

      {/* Mobile add button */}
      {canUpload && media && (
        <span className={styles.mobileAdd}>
          <Button
            variant="warm"
            iconOnly
            icon={<Upload size={20} aria-hidden="true" />}
            aria-label={t('trip.addPhotos')}
            onClick={() => fileInputRef.current?.click()}
          />
        </span>
      )}

      {/* ── Lightbox ── */}
      {lightboxIndex !== null && sortedMedia && user && members && (
        <Lightbox
          media={sortedMedia}
          index={lightboxIndex}
          onIndexChange={onLightboxIndex}
          onClose={closeLightbox}
          uploaderName={(id) => members.find((m) => m.userId === id)?.displayName ?? '—'}
          viewerId={user.id}
          isAlbumOwner={isOwner}
          originRect={originRectRef.current}
        />
      )}

      {/* ── Share sheet (owner) ── */}
      {album && (
        <Modal
          open={shareOpen}
          onClose={() => setShareOpen(false)}
          title={t('trip.share.title', { title: album.title })}
          wide
        >
          <ModalBody>
            {album.visibility === 'PRIVATE' ? (
              <>
                <p className="t-body">{t('trip.share.private.body')}</p>
                <Button
                  onClick={async () => {
                    const resp = await updateAlbum(albumId, { visibility: 'LINK_SHARED' }).catch(
                      () => null,
                    );
                    if (resp?.success && resp.data) setAlbum(resp.data);
                    else toast({ kind: 'error', title: t('common.errorGeneric') });
                  }}
                >
                  {t('trip.share.enable')}
                </Button>
              </>
            ) : (
              <>
                <p className="t-body-s" style={{ color: 'var(--text-2)' }}>
                  {t('trip.share.enabled.body')}
                </p>
                <div
                  style={{
                    display: 'flex',
                    gap: 10,
                    alignItems: 'center',
                    background: 'var(--bg-sunken)',
                    borderRadius: 'var(--r-sm)',
                    padding: 12,
                  }}
                >
                  <span
                    style={{
                      flex: 1,
                      minWidth: 0,
                      fontFamily: 'ui-monospace, monospace',
                      fontSize: 12.5,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {publicUrl}
                  </span>
                  <Button size="sm" onClick={() => void copyShare()}>
                    {t('common.copy')}
                  </Button>
                </div>
              </>
            )}
          </ModalBody>
        </Modal>
      )}

      {/* ── Leave trip (non-owner, via overflow) ── */}
      <Confirm
        open={leaveOpen}
        onClose={() => setLeaveOpen(false)}
        onConfirm={leaveTrip}
        title={t('settings.leave.title', { title: album?.title ?? '' })}
        body={t('settings.leave.body')}
        confirmLabel={t('settings.leave.cta')}
        cancelLabel={t('common.cancel')}
        busy={leaveBusy}
      />
    </div>
  );
}

function StripCard({
  event,
  onOpen,
}: {
  event: ActivityEventDto | DerivedActivityEvent;
  onOpen: (mediaId: string | undefined) => void;
}) {
  const { t } = useLocale();
  const rel = relativeTimeParts(event.createdAt);
  const time =
    rel.kind === 'justNow'
      ? t('time.justNow')
      : rel.kind === 'minutes'
        ? t('time.minutesAgo', { n: rel.n })
        : rel.kind === 'hours'
          ? t('time.hoursAgo', { n: rel.n })
          : rel.kind === 'yesterday'
            ? t('time.yesterday')
            : rel.kind === 'days'
              ? t('time.daysAgo', { n: rel.n })
              : '';

  const firstName = event.actor.displayName.split(/\s+/)[0];
  const isAdded = event.type === 'MEDIA_ADDED';
  const thumb =
    'thumbnailUrls' in event && event.thumbnailUrls && event.thumbnailUrls.length > 0
      ? event.thumbnailUrls[0]
      : undefined;

  let line: string;
  let meta = time;
  switch (event.type) {
    case 'MEDIA_ADDED':
      line = t('trip.activityStrip.added', { name: firstName, n: event.count ?? 1 });
      break;
    case 'MEMBER_JOINED':
      line = t('trip.activityStrip.joined', { name: firstName });
      break;
    case 'COMMENT':
      line = t('trip.activityStrip.commented', { name: firstName });
      meta =
        'commentBody' in event && event.commentBody
          ? `“${event.commentBody.slice(0, 32)}” · ${time}`
          : time;
      break;
    case 'REACTION':
      line = t('trip.activityStrip.reacted', { name: firstName });
      meta =
        'reactionType' in event && event.reactionType
          ? `${REACTION_EMOJI[event.reactionType]} · ${time}`
          : time;
      break;
    default:
      line = firstName;
  }

  const nameEnd = line.indexOf(firstName) + firstName.length;

  return (
    <button
      type="button"
      className={[styles.stripCard, isAdded ? styles.stripCardWarm : ''].filter(Boolean).join(' ')}
      onClick={() => onOpen('mediaIds' in event ? event.mediaIds?.[0] : undefined)}
    >
      <Avatar
        userId={event.actor.userId}
        displayName={event.actor.displayName}
        avatarUrl={event.actor.avatarUrl ?? undefined}
        size="sm"
        decorative
      />
      <span>
        <span className={styles.stripLine}>
          <span className={styles.stripName}>{line.slice(0, nameEnd)}</span>
          {line.slice(nameEnd)}
        </span>
        <span className={styles.stripMeta}>{meta}</span>
      </span>
      {thumb && <img className={styles.stripThumb} src={thumb} alt="" aria-hidden="true" />}
    </button>
  );
}
