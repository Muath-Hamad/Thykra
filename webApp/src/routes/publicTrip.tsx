import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { createPortal } from 'react-dom';
import { Link, useParams } from '@tanstack/react-router';
import {
  ChevronLeft,
  ChevronRight,
  Minus,
  Pause,
  Play,
  Plus,
  X,
} from 'lucide-react';
import {
  getPublicAlbum,
  type PublicAlbumViewDto,
  type PublicMediaDto,
} from '../api/publicAlbums';
import { REACTION_EMOJI } from '../api/reactions';
import { useLocale } from '../i18n/LocaleProvider';
import { groupIntoChapters } from '../lib/chapters';
import { formatDateTime, formatMonthYear } from '../lib/format';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { MediaGallery } from '../ui/MediaGallery';
import { Plate } from '../ui/Plate';
import { Delayed, Skeleton } from '../ui/Skeleton';
import { Stamp } from '../ui/Stamp';
import { useDocumentTitle, useMediaQuery, usePrefersReducedMotion } from '../ui/hooks';
import styles from './publicTrip.module.css';

/** The public top bar is 52px — the gallery pins its chapter rules under it. */
const TOP_BAR_H = 52;

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ok'; view: PublicAlbumViewDto }
  /** visibility flipped back to PRIVATE — envelope came back {success:false}. */
  | { kind: 'revoked' }
  | { kind: 'error' };

/**
 * /public/:albumId and /s/:albumId — the same component on both paths.
 * No AppShell: a signed-out visitor gets a 52px bar, the album, and one
 * closing invitation. Every control she cannot use is absent, not disabled.
 */
export function PublicTripPage() {
  const { albumId } = useParams({ strict: false }) as { albumId: string };
  const { t, lang } = useLocale();
  const isMobile = useMediaQuery('(max-width: 767px)');

  const [state, setState] = useState<LoadState>({ kind: 'loading' });
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  // Focus-return registry — the lightbox hands focus back to the exact plate.
  const plateRefs = useRef(new Map<string, HTMLButtonElement>());

  const load = useCallback(async () => {
    setState({ kind: 'loading' });
    try {
      const resp = await getPublicAlbum(albumId);
      if (resp.success && resp.data) setState({ kind: 'ok', view: resp.data });
      else setState({ kind: 'revoked' });
    } catch {
      setState({ kind: 'error' });
    }
  }, [albumId]);

  useEffect(() => {
    void load();
  }, [load]);

  useDocumentTitle(state.kind === 'ok' ? `${state.view.album.title} · Thykra` : null);

  const view = state.kind === 'ok' ? state.view : null;

  // Lightbox order must match the gallery's reading order exactly.
  const ordered = useMemo(
    () => (view ? groupIntoChapters(view.media).flatMap((c) => c.items) : []),
    [view],
  );

  const openLightbox = useCallback(
    (item: PublicMediaDto) => {
      const idx = ordered.findIndex((m) => m.id === item.id);
      if (idx >= 0) setLightboxIndex(idx);
    },
    [ordered],
  );

  const closeLightbox = useCallback(() => {
    const current = lightboxIndex !== null ? ordered[lightboxIndex] : null;
    setLightboxIndex(null);
    if (current) {
      requestAnimationFrame(() => plateRefs.current.get(current.id)?.focus());
    }
  }, [lightboxIndex, ordered]);

  return (
    <div className="page-enter">
      <a href="#content" className="skip-link">
        {t('app.skipToContent')}
      </a>

      {/* ── Own top bar — this route has no app chrome ── */}
      <header className={styles.topBar}>
        <Link to="/" className={styles.wordmark}>
          {t('app.name')}
        </Link>
        <Link to="/login" className={styles.signIn}>
          {t('nav.signIn')}
        </Link>
      </header>

      <main id="content">
        {state.kind === 'revoked' ? (
          /* No cover, no title, no count — the album is not shared any more. */
          <div className={styles.fullState}>
            <EmptyState title={t('public.revoked.title')} body={t('public.revoked.body')} />
          </div>
        ) : state.kind === 'error' ? (
          <div className={styles.fullState}>
            <EmptyState
              kind="error"
              title={t('public.error.title')}
              actions={<Button onClick={() => void load()}>{t('common.retry')}</Button>}
            />
          </div>
        ) : (
          <>
            <div className={styles.page}>
              {/* ── Masthead ── */}
              <div className={styles.masthead}>
                <div className={styles.mastText}>
                  {view ? (
                    <>
                      <div className={styles.stampRow}>
                        <Stamp
                          tone="ink"
                          eyebrow={t('public.stamp')}
                          name={view.album.ownerDisplayName}
                          avatar={
                            <Avatar
                              userId={view.album.id}
                              displayName={view.album.ownerDisplayName}
                              avatarUrl={view.album.ownerAvatarUrl}
                              size="sm"
                              decorative
                            />
                          }
                        />
                      </div>
                      <h1 className={`t-display-l ${styles.title}`}>{view.album.title}</h1>
                      {view.album.description && (
                        <p className={`t-body-l ${styles.description}`}>
                          {view.album.description}
                        </p>
                      )}
                      {/* No member count — a public viewer does not get the guest list. */}
                      <p className={`t-body-s ${styles.meta}`}>
                        {t('public.meta', {
                          n: view.album.mediaCount,
                          date: formatMonthYear(view.album.createdAt, lang),
                        })}
                      </p>
                    </>
                  ) : (
                    <Delayed>
                      <div className={styles.mastSkeleton} aria-hidden="true">
                        <Skeleton width={168} height={34} radius="var(--r-pill)" />
                        <Skeleton width="72%" height={48} radius={6} />
                        <Skeleton width="52%" height={16} />
                        <Skeleton width={180} height={13} />
                      </div>
                    </Delayed>
                  )}
                </div>
                <div className={styles.coverCell}>
                  {view?.album.coverUrl ? (
                    <Plate
                      src={view.album.coverUrl}
                      alt=""
                      aspectRatio={isMobile ? '16 / 9' : '4 / 3'}
                      eager
                    />
                  ) : view ? null : (
                    <Delayed>
                      <Skeleton aspectRatio="4 / 3" radius={0} />
                    </Delayed>
                  )}
                </div>
              </div>

              {/* ── Chapters ── */}
              <div className={styles.gallerySection}>
                {!view && (
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

                {view && view.media.length === 0 && (
                  <EmptyState
                    title={t('public.empty.title')}
                    body={t('public.empty.body', { name: view.album.ownerDisplayName })}
                  />
                )}

                {view && view.media.length > 0 && (
                  <MediaGallery
                    media={view.media}
                    animKey={albumId}
                    stickyTop={TOP_BAR_H}
                    onOpen={openLightbox}
                    registerItem={(id, el) => {
                      if (el) plateRefs.current.set(id, el);
                      else plateRefs.current.delete(id);
                    }}
                    labelFor={(item) =>
                      `${item.type === 'VIDEO' ? t('media.video') : t('media.photo')} — ${formatDateTime(
                        item.takenAt ?? item.uploadedAt,
                        lang,
                      )}`
                    }
                    chipsFor={(item) => {
                      const summary = item.reactionSummary;
                      if (!summary || summary.length === 0) return undefined;
                      return [...summary]
                        .sort((a, b) => b.count - a.count)
                        .slice(0, 2)
                        .map((s) => `${REACTION_EMOJI[s.type]} ${s.count}`);
                    }}
                  />
                )}
              </div>
            </div>

            {/* ── Closing conversion band — full-bleed ink ── */}
            {view && (
              <section className={styles.closing}>
                <div className={styles.closingInner}>
                  <div className={styles.closingText}>
                    {/*
                      public.closing.label needs a member count ({m}) and the
                      public payload deliberately withholds it — so the label
                      line is dropped rather than printed with a placeholder.
                    */}
                    <h2 className={`t-display-s ${styles.closingTitle}`}>
                      {t('public.closing.title')}
                    </h2>
                    <p className={styles.closingBody}>{t('public.closing.body')}</p>
                  </div>
                  <div className={styles.closingCta}>
                    <Button as="a" href="/" size="lg">
                      {t('public.closing.cta')}
                    </Button>
                  </div>
                </div>
              </section>
            )}
          </>
        )}
      </main>

      {lightboxIndex !== null && ordered.length > 0 && (
        <PublicLightbox
          media={ordered}
          index={lightboxIndex}
          onIndexChange={setLightboxIndex}
          onClose={closeLightbox}
        />
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════
   Public lightbox — deliberately NOT ui/Lightbox: that one carries
   comments, reactions and download, all of which are member-only.
   What survives here: navigation, zoom, slideshow, and the date.
   ═══════════════════════════════════════════════════════════ */

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])';

const SLIDESHOW_MS = 4000;
const ZOOM_STEPS = [1, 2, 3];

function PublicLightbox({
  media,
  index,
  onIndexChange,
  onClose,
}: {
  media: PublicMediaDto[];
  index: number;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}) {
  const { t, lang } = useLocale();
  const reducedMotion = usePrefersReducedMotion();
  const rootRef = useRef<HTMLDivElement | null>(null);

  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [playing, setPlaying] = useState(false);

  const item: PublicMediaDto | undefined = media[index];

  // Every new photo starts unzoomed and un-panned.
  useEffect(() => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  }, [index]);

  const stepZoom = useCallback((direction: 1 | -1) => {
    setZoom((z) => {
      const at = ZOOM_STEPS.indexOf(z);
      const nextIdx = Math.min(
        ZOOM_STEPS.length - 1,
        Math.max(0, (at < 0 ? 0 : at) + direction),
      );
      const next = ZOOM_STEPS[nextIdx];
      if (next === 1) setPan({ x: 0, y: 0 });
      return next;
    });
  }, []);

  const cycleZoom = useCallback(() => {
    setZoom((z) => {
      const at = ZOOM_STEPS.indexOf(z);
      const next = ZOOM_STEPS[(at < 0 ? 0 : at + 1) % ZOOM_STEPS.length];
      if (next === 1) setPan({ x: 0, y: 0 });
      return next;
    });
  }, []);

  const goTo = useCallback(
    (next: number) => {
      if (next < 0 || next >= media.length) return;
      onIndexChange(next);
    },
    [media.length, onIndexChange],
  );

  // Slideshow — 4s, stops at the last item, absent under reduced motion.
  useEffect(() => {
    if (!playing || reducedMotion) return;
    if (index >= media.length - 1) {
      setPlaying(false);
      return;
    }
    const timer = window.setTimeout(() => onIndexChange(index + 1), SLIDESHOW_MS);
    return () => window.clearTimeout(timer);
  }, [playing, index, media.length, onIndexChange, reducedMotion]);

  // ── Keyboard ──
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      // Arrows mirror under RTL — the document's direction is the source.
      const rtl = document.documentElement.dir === 'rtl';
      switch (e.key) {
        case 'Escape':
          e.preventDefault();
          if (zoom > 1) {
            setZoom(1);
            setPan({ x: 0, y: 0 });
          } else {
            onClose();
          }
          break;
        case 'ArrowRight':
          e.preventDefault();
          if (zoom > 1) setPan((p) => ({ ...p, x: p.x - 40 }));
          else {
            setPlaying(false);
            goTo(rtl ? index - 1 : index + 1);
          }
          break;
        case 'ArrowLeft':
          e.preventDefault();
          if (zoom > 1) setPan((p) => ({ ...p, x: p.x + 40 }));
          else {
            setPlaying(false);
            goTo(rtl ? index + 1 : index - 1);
          }
          break;
        case 'ArrowUp':
          if (zoom > 1) {
            e.preventDefault();
            setPan((p) => ({ ...p, y: p.y + 40 }));
          }
          break;
        case 'ArrowDown':
          if (zoom > 1) {
            e.preventDefault();
            setPan((p) => ({ ...p, y: p.y - 40 }));
          }
          break;
        case '+':
        case '=':
          e.preventDefault();
          stepZoom(1);
          break;
        case '-':
          e.preventDefault();
          stepZoom(-1);
          break;
        case '0':
          e.preventDefault();
          setZoom(1);
          setPan({ x: 0, y: 0 });
          break;
        case ' ':
          if (!reducedMotion && item?.type !== 'VIDEO') {
            e.preventDefault();
            setPlaying((p) => !p);
          }
          break;
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [goTo, index, item, onClose, reducedMotion, stepZoom, zoom]);

  // Focus enters the dialog; the caller returns it to the opening plate.
  useEffect(() => {
    rootRef.current?.focus();
  }, []);

  // Basic trap — Tab cycles inside the dialog and never escapes to the page.
  const onTrapKeyDown = (e: ReactKeyboardEvent) => {
    if (e.key !== 'Tab') return;
    const root = rootRef.current;
    if (!root) return;
    const focusables = [...root.querySelectorAll<HTMLElement>(FOCUSABLE)].filter(
      (el) => el.offsetParent !== null,
    );
    if (focusables.length === 0) return;
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
  };

  if (!item) return null;

  const rtl = document.documentElement.dir === 'rtl';
  const when = formatDateTime(item.takenAt ?? item.uploadedAt, lang);

  return createPortal(
    // data-theme="dark" so every token inside resolves to Darkroom.
    <div
      ref={rootRef}
      className={styles.lbRoot}
      data-theme="dark"
      role="dialog"
      aria-modal="true"
      aria-label={when}
      tabIndex={-1}
      onKeyDown={onTrapKeyDown}
    >
      <div className={styles.lbStage}>
        <div
          className={styles.lbMediaBox}
          style={{
            transform: zoom > 1 ? `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` : undefined,
          }}
        >
          {item.type === 'VIDEO' ? (
            <video src={item.url} poster={item.thumbnailUrl} controls />
          ) : (
            <img src={item.url} alt="" draggable={false} />
          )}
        </div>
      </div>

      {/* Close */}
      <div className={`${styles.pill} ${styles.pillTopLeading}`}>
        <button
          type="button"
          className={styles.iconButton}
          onClick={onClose}
          aria-label={t('common.close')}
        >
          <X size={20} aria-hidden="true" />
        </button>
      </div>

      {/* Zoom + slideshow */}
      <div className={`${styles.pill} ${styles.pillTopTrailing}`}>
        <button
          type="button"
          className={styles.iconButton}
          onClick={cycleZoom}
          aria-label={t('lightbox.zoomIn')}
        >
          <Plus size={20} aria-hidden="true" />
        </button>
        {zoom > 1 && (
          <button
            type="button"
            className={styles.iconButton}
            onClick={() => stepZoom(-1)}
            aria-label={t('lightbox.zoomOut')}
          >
            <Minus size={20} aria-hidden="true" />
          </button>
        )}
        {/* Slideshow is absent — not disabled — under reduced motion. */}
        {!reducedMotion && item.type !== 'VIDEO' && (
          <button
            type="button"
            className={styles.iconButton}
            onClick={() => setPlaying((p) => !p)}
            aria-label={t('lightbox.slideshow')}
            aria-pressed={playing}
          >
            {playing ? (
              <Pause size={20} aria-hidden="true" />
            ) : (
              <Play size={20} aria-hidden="true" />
            )}
          </button>
        )}
      </div>

      {/* Previous / next follow reading order */}
      {index > 0 && (
        <button
          type="button"
          className={`${styles.pill} ${styles.navButton} ${styles.navPrev}`}
          onClick={() => {
            setPlaying(false);
            goTo(index - 1);
          }}
          aria-label={t('lightbox.previous')}
        >
          {rtl ? (
            <ChevronRight size={20} aria-hidden="true" />
          ) : (
            <ChevronLeft size={20} aria-hidden="true" />
          )}
        </button>
      )}
      {index < media.length - 1 && (
        <button
          type="button"
          className={`${styles.pill} ${styles.navButton} ${styles.navNext}`}
          onClick={() => {
            setPlaying(false);
            goTo(index + 1);
          }}
          aria-label={t('lightbox.next')}
        >
          {rtl ? (
            <ChevronLeft size={20} aria-hidden="true" />
          ) : (
            <ChevronRight size={20} aria-hidden="true" />
          )}
        </button>
      )}

      {/* Info — the date, and nothing a signed-out viewer cannot act on */}
      <div className={styles.infoRow}>
        <span className={`${styles.pill} ${styles.infoPill}`}>{when}</span>
      </div>
    </div>,
    document.body,
  );
}
