import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';
import {
  X,
  Plus,
  Minus,
  Play,
  Pause,
  Download,
  Share2,
  MessageCircle,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import type { MediaDto } from '../api/media';
import {
  getComments,
  addComment,
  updateComment,
  deleteComment,
  type CommentDto,
  MAX_COMMENT_LENGTH,
} from '../api/comments';
import {
  getReactions,
  toggleReaction,
  REACTION_EMOJI,
  REACTION_TYPES,
  type ReactionType,
  type ReactionSummaryDto,
} from '../api/reactions';
import { useLocale } from '../i18n/LocaleProvider';
import {
  formatDateTime,
  formatFileSize,
  relativeTimeParts,
} from '../lib/format';
import { usePrefersReducedMotion } from './hooks';
import { Avatar } from './Avatar';
import { Button } from './Button';
import { Confirm } from './Modal';
import { useToast } from './Toast';
import styles from './Lightbox.module.css';

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

// Comments closed by default; the choice persists for the session so someone
// reading a conversation is not re-closing it on every photo.
let commentsOpenSession = false;

const IDLE_MS = 2500;
const SLIDESHOW_MS = 4000;

export interface LightboxProps {
  media: MediaDto[];
  index: number;
  onIndexChange: (index: number) => void;
  onClose: () => void;
  uploaderName: (uploaderId: string) => string;
  viewerId: string;
  isAlbumOwner: boolean;
  /** Rect of the plate that opened us — the shared-element origin. */
  originRect?: DOMRect | null;
}

export function Lightbox({
  media,
  index,
  onIndexChange,
  onClose,
  uploaderName,
  viewerId,
  isAlbumOwner,
  originRect,
}: LightboxProps) {
  const { t, lang, dir } = useLocale();
  const { toast } = useToast();
  const reducedMotion = usePrefersReducedMotion();
  const item = media[index];

  const rootRef = useRef<HTMLDivElement | null>(null);
  const mediaBoxRef = useRef<HTMLDivElement | null>(null);

  // ── Chrome idle hiding ──
  const [chromeVisible, setChromeVisible] = useState(true);
  const idleTimer = useRef<number | null>(null);
  const wake = useCallback(() => {
    setChromeVisible(true);
    if (idleTimer.current) window.clearTimeout(idleTimer.current);
    idleTimer.current = window.setTimeout(() => setChromeVisible(false), IDLE_MS);
  }, []);
  useEffect(() => {
    wake();
    return () => {
      if (idleTimer.current) window.clearTimeout(idleTimer.current);
    };
  }, [wake]);

  // ── Zoom & pan ──
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  useEffect(() => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  }, [index]);

  const zoomBy = useCallback((factor: number) => {
    setZoom((z) => {
      const next = Math.min(4, Math.max(1, z * factor));
      if (next === 1) setPan({ x: 0, y: 0 });
      return next;
    });
  }, []);

  // ── Slideshow — 4s, pauses on input, stops at the last item, and is
  // disabled entirely under prefers-reduced-motion. ──
  const [playing, setPlaying] = useState(false);
  useEffect(() => {
    if (!playing || reducedMotion) return;
    if (index >= media.length - 1) {
      setPlaying(false);
      return;
    }
    const timer = window.setTimeout(() => onIndexChange(index + 1), SLIDESHOW_MS);
    return () => window.clearTimeout(timer);
  }, [playing, index, media.length, onIndexChange, reducedMotion]);

  const pauseSlideshow = useCallback(() => setPlaying(false), []);

  // ── Full image loading: thumbnail first, swap on decode ──
  const [fullLoaded, setFullLoaded] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadNonce, setLoadNonce] = useState(0);
  useEffect(() => {
    setFullLoaded(false);
    setLoadFailed(false);
    if (!item || item.type === 'VIDEO') return;
    let cancelled = false;
    const img = new Image();
    img.src = item.url;
    img
      .decode()
      .then(() => {
        if (!cancelled) setFullLoaded(true);
      })
      .catch(() => {
        if (!cancelled) {
          // decode() can reject for CORS-tainted but displayable images —
          // fall back to onload semantics before declaring failure.
          if (img.complete && img.naturalWidth > 0) setFullLoaded(true);
          else setLoadFailed(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [item, loadNonce]);

  // ── Comments ──
  const [commentsOpen, setCommentsOpen] = useState(commentsOpenSession);
  const setCommentsOpenPersist = useCallback((open: boolean) => {
    commentsOpenSession = open;
    setCommentsOpen(open);
  }, []);
  const [comments, setComments] = useState<CommentDto[] | null>(null);
  const [pendingComment, setPendingComment] = useState<{ body: string; failed: boolean } | null>(
    null,
  );
  const [draft, setDraft] = useState('');
  const [editing, setEditing] = useState<{ id: string; body: string } | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CommentDto | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  useEffect(() => {
    setComments(null);
    setPendingComment(null);
    setEditing(null);
    if (!item) return;
    let cancelled = false;
    getComments(item.albumId, item.id).then((resp) => {
      if (!cancelled && resp.success && resp.data) setComments(resp.data);
    });
    return () => {
      cancelled = true;
    };
  }, [item]);

  const postComment = useCallback(async () => {
    const body = draft.trim();
    if (!body || !item) return;
    setDraft('');
    setPendingComment({ body, failed: false });
    const resp = await addComment(item.albumId, item.id, body).catch(() => null);
    if (resp?.success && resp.data) {
      setComments((prev) => [...(prev ?? []), resp.data!]);
      setPendingComment(null);
    } else {
      // The text is never lost — it stays in place with a Retry.
      setPendingComment({ body, failed: true });
      toast({ kind: 'error', title: t('lightbox.commentFailed') });
    }
  }, [draft, item, t, toast]);

  const retryComment = useCallback(async () => {
    if (!pendingComment || !item) return;
    const body = pendingComment.body;
    setPendingComment({ body, failed: false });
    const resp = await addComment(item.albumId, item.id, body).catch(() => null);
    if (resp?.success && resp.data) {
      setComments((prev) => [...(prev ?? []), resp.data!]);
      setPendingComment(null);
    } else {
      setPendingComment({ body, failed: true });
    }
  }, [pendingComment, item]);

  const saveEdit = useCallback(async () => {
    if (!editing || !item) return;
    const body = editing.body.trim();
    if (!body) return;
    const target = editing.id;
    setEditing(null);
    const prev = comments;
    setComments(
      (list) => list?.map((c) => (c.id === target ? { ...c, body, updatedAt: new Date().toISOString() } : c)) ?? null,
    );
    const resp = await updateComment(item.albumId, item.id, target, body).catch(() => null);
    if (!resp?.success) {
      setComments(prev ?? null);
      toast({ kind: 'error', title: t('lightbox.commentFailed') });
    }
  }, [comments, editing, item, t, toast]);

  const confirmDelete = useCallback(async () => {
    if (!deleteTarget || !item) return;
    setDeleteBusy(true);
    const resp = await deleteComment(item.albumId, item.id, deleteTarget.id).catch(() => null);
    setDeleteBusy(false);
    if (resp?.success) {
      setComments((list) => list?.filter((c) => c.id !== deleteTarget.id) ?? null);
      setDeleteTarget(null);
    } else {
      toast({ kind: 'error', title: t('common.errorGeneric') });
    }
  }, [deleteTarget, item, t, toast]);

  // ── Reactions ──
  const [summary, setSummary] = useState<ReactionSummaryDto[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  useEffect(() => {
    setSummary([]);
    setPickerOpen(false);
    if (!item) return;
    let cancelled = false;
    getReactions(item.albumId, item.id).then((resp) => {
      if (!cancelled && resp.success && resp.data) setSummary(resp.data.summary);
    });
    return () => {
      cancelled = true;
    };
  }, [item]);

  const react = useCallback(
    async (type: ReactionType) => {
      if (!item) return;
      // Optimistic; rollback with a toast on failure.
      const before = summary;
      setSummary((prev) => {
        const existing = prev.find((s) => s.type === type);
        if (existing) {
          const nextCount = existing.reactedByMe ? existing.count - 1 : existing.count + 1;
          return prev
            .map((s) =>
              s.type === type ? { ...s, count: nextCount, reactedByMe: !s.reactedByMe } : s,
            )
            .filter((s) => s.count > 0);
        }
        return [...prev, { type, count: 1, reactedByMe: true }];
      });
      const resp = await toggleReaction(item.albumId, item.id, type).catch(() => null);
      if (!resp?.success) {
        setSummary(before);
        toast({ kind: 'error', title: t('lightbox.reactionFailed') });
      } else if (resp.data) {
        setSummary(resp.data.summary);
      }
    },
    [item, summary, t, toast],
  );

  // ── Navigation ──
  const goTo = useCallback(
    (next: number) => {
      if (next < 0 || next >= media.length) return;
      pauseSlideshow();
      onIndexChange(next);
    },
    [media.length, onIndexChange, pauseSlideshow],
  );

  const download = useCallback(() => {
    if (!item) return;
    const a = document.createElement('a');
    a.href = item.url;
    a.download = item.filename;
    a.rel = 'noopener';
    document.body.appendChild(a);
    a.click();
    a.remove();
  }, [item]);

  const shareMedia = useCallback(async () => {
    if (!item) return;
    const url = `${window.location.origin}/trips/${item.albumId}?m=${item.id}`;
    try {
      await navigator.clipboard.writeText(url);
      toast({ kind: 'success', title: t('trip.share.copied') });
    } catch {
      toast({ kind: 'error', title: t('common.errorGeneric') });
    }
  }, [item, t, toast]);

  // ── Keyboard — the complete map ──
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      // Don't hijack typing.
      const target = e.target as HTMLElement;
      const typing = target.tagName === 'TEXTAREA' || target.tagName === 'INPUT';
      wake();
      if (typing && e.key !== 'Escape') return;

      const rtl = dir === 'rtl';
      switch (e.key) {
        case 'Escape':
          e.preventDefault();
          if (zoom > 1) {
            setZoom(1);
            setPan({ x: 0, y: 0 });
          } else if (typing) {
            (target as HTMLElement).blur();
          } else {
            onClose();
          }
          break;
        case 'ArrowRight':
          e.preventDefault();
          if (zoom > 1) setPan((p) => ({ ...p, x: p.x - 40 }));
          else goTo(rtl ? index - 1 : index + 1);
          break;
        case 'ArrowLeft':
          e.preventDefault();
          if (zoom > 1) setPan((p) => ({ ...p, x: p.x + 40 }));
          else goTo(rtl ? index + 1 : index - 1);
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
        case 'Home':
          e.preventDefault();
          goTo(0);
          break;
        case 'End':
          e.preventDefault();
          goTo(media.length - 1);
          break;
        case '+':
        case '=':
          e.preventDefault();
          zoomBy(1.5);
          break;
        case '-':
          e.preventDefault();
          zoomBy(1 / 1.5);
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
        case 'c':
        case 'C':
          e.preventDefault();
          setCommentsOpenPersist(!commentsOpenSession);
          break;
        case 'r':
        case 'R':
          e.preventDefault();
          setPickerOpen((o) => !o);
          break;
        case 'd':
        case 'D':
          e.preventDefault();
          download();
          break;
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [
    dir,
    download,
    goTo,
    index,
    item,
    media.length,
    onClose,
    reducedMotion,
    setCommentsOpenPersist,
    wake,
    zoom,
    zoomBy,
  ]);

  // ── Focus trap + focus entry ──
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    rootRef.current?.focus();
    return () => {
      previous?.focus?.();
    };
  }, []);

  const onTrapKeyDown = (e: React.KeyboardEvent) => {
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

  // ── Shared-element open: origin rect → final rect, 320ms, one transform ──
  useEffect(() => {
    if (!originRect || reducedMotion) return;
    const box = mediaBoxRef.current;
    if (!box) return;
    const raf = requestAnimationFrame(() => {
      const final = box.getBoundingClientRect();
      if (final.width === 0 || final.height === 0) return;
      const dx = originRect.left + originRect.width / 2 - (final.left + final.width / 2);
      const dy = originRect.top + originRect.height / 2 - (final.top + final.height / 2);
      const scale = originRect.width / final.width;
      box.animate(
        [
          { transform: `translate(${dx}px, ${dy}px) scale(${scale})` },
          { transform: 'none' },
        ],
        { duration: 320, easing: 'cubic-bezier(.2,.8,.28,1)' },
      );
    });
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Touch: horizontal swipe changes photo, vertical swipe-down closes ──
  const touchStart = useRef<{ x: number; y: number } | null>(null);
  const onTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 1) {
      touchStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }
  };
  const onTouchEnd = (e: React.TouchEvent) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start || zoom > 1) return;
    const dx = e.changedTouches[0].clientX - start.x;
    const dy = e.changedTouches[0].clientY - start.y;
    if (Math.abs(dy) > 80 && Math.abs(dy) > Math.abs(dx) * 1.5 && dy > 0) {
      onClose();
    } else if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.5) {
      const rtl = dir === 'rtl';
      const forward = rtl ? dx > 0 : dx < 0;
      goTo(forward ? index + 1 : index - 1);
    }
  };

  if (!item) return null;

  const commentCount = (comments?.length ?? 0) + (pendingComment ? 1 : 0);
  const takenLabel = item.takenAt
    ? formatDateTime(item.takenAt, lang)
    : t('lightbox.addedOn', { date: formatDateTime(item.uploadedAt, lang) });
  const dims = item.width && item.height ? ` · ${item.width}×${item.height}` : '';
  const shownSummary = summary.slice(0, 3);

  return createPortal(
    <div
      ref={rootRef}
      className={styles.root}
      data-theme="dark"
      role="dialog"
      aria-modal="true"
      aria-label={item.filename}
      tabIndex={-1}
      onKeyDown={onTrapKeyDown}
      onMouseMove={wake}
      onTouchStart={onTouchStart}
      onTouchEnd={onTouchEnd}
    >
      <div className={styles.stage}>
        <div className={styles.mediaArea}>
          {item.type === 'VIDEO' ? (
            <div ref={mediaBoxRef} className={styles.mediaBox}>
              <video src={item.url} poster={item.thumbnailUrl ?? undefined} controls autoPlay={false} />
            </div>
          ) : loadFailed ? (
            <div className={styles.failed} role="alert">
              <div className={styles.failedPlate} aria-hidden="true" />
              <div>{t('lightbox.imageFailed', { filename: item.filename })}</div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  setLoadFailed(false);
                  setLoadNonce((n) => n + 1);
                }}
              >
                {t('common.retry')}
              </Button>
            </div>
          ) : (
            <div
              ref={mediaBoxRef}
              className={styles.mediaBox}
              style={{
                transform:
                  zoom > 1 ? `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` : undefined,
                cursor: zoom > 1 ? 'grab' : undefined,
              }}
              onDoubleClick={() => (zoom > 1 ? (setZoom(1), setPan({ x: 0, y: 0 })) : zoomBy(2))}
            >
              {!fullLoaded && <div className={styles.progressLine} aria-hidden="true" />}
              <img
                src={fullLoaded ? item.url : item.thumbnailUrl ?? item.url}
                alt={item.filename}
                className={fullLoaded ? undefined : styles.loadingThumb}
                draggable={false}
              />
            </div>
          )}
        </div>

        <div className={[styles.chrome, chromeVisible ? '' : styles.chromeHidden].filter(Boolean).join(' ')}>
          {/* Top leading: close + info */}
          <div className={`${styles.pill} ${styles.topLeading}`}>
            <button type="button" className={styles.iconButton} onClick={onClose} aria-label={t('common.close')}>
              <X size={20} aria-hidden="true" />
            </button>
            <span className={styles.info}>
              <span className={styles.infoName}>{item.filename}</span>
              <span className={styles.infoMeta} style={{ display: 'block' }}>
                {uploaderName(item.uploaderId)} · {takenLabel} · {formatFileSize(item.fileSize)}
                {dims}
              </span>
            </span>
          </div>

          {/* Top trailing: zoom, slideshow, download, share, comments */}
          <div className={`${styles.pill} ${styles.topTrailing}`}>
            <button
              type="button"
              className={styles.iconButton}
              onClick={() => zoomBy(1.5)}
              aria-label={t('lightbox.zoomIn')}
            >
              <Plus size={20} aria-hidden="true" />
            </button>
            {zoom > 1 && (
              <button
                type="button"
                className={styles.iconButton}
                onClick={() => zoomBy(1 / 1.5)}
                aria-label={t('lightbox.zoomOut')}
              >
                <Minus size={20} aria-hidden="true" />
              </button>
            )}
            {!reducedMotion && item.type !== 'VIDEO' && (
              <button
                type="button"
                className={styles.iconButton}
                onClick={() => setPlaying((p) => !p)}
                aria-label={t('lightbox.slideshow')}
                aria-pressed={playing}
              >
                {playing ? <Pause size={20} aria-hidden="true" /> : <Play size={20} aria-hidden="true" />}
              </button>
            )}
            <button type="button" className={styles.iconButton} onClick={download} aria-label={t('lightbox.download')}>
              <Download size={20} aria-hidden="true" />
            </button>
            <button
              type="button"
              className={styles.iconButton}
              onClick={shareMedia}
              aria-label={t('lightbox.shareMedia')}
            >
              <Share2 size={20} aria-hidden="true" />
            </button>
            <button
              type="button"
              className={[styles.iconButton, commentsOpen ? styles.iconButtonActive : '']
                .filter(Boolean)
                .join(' ')}
              onClick={() => setCommentsOpenPersist(!commentsOpen)}
              aria-label={t('lightbox.comments')}
              aria-pressed={commentsOpen}
            >
              {commentCount > 0 ? (
                <span className={styles.commentCount}>{commentCount}</span>
              ) : (
                <MessageCircle size={20} aria-hidden="true" />
              )}
            </button>
          </div>

          {/* Sides: previous / next follow reading order */}
          {index > 0 && (
            <button
              type="button"
              className={`${styles.navButton} ${styles.navPrev}`}
              onClick={() => goTo(index - 1)}
              aria-label={t('lightbox.previous')}
            >
              {dir === 'rtl' ? <ChevronRight size={20} aria-hidden="true" /> : <ChevronLeft size={20} aria-hidden="true" />}
            </button>
          )}
          {index < media.length - 1 && (
            <button
              type="button"
              className={`${styles.navButton} ${styles.navNext}`}
              onClick={() => goTo(index + 1)}
              aria-label={t('lightbox.next')}
            >
              {dir === 'rtl' ? <ChevronLeft size={20} aria-hidden="true" /> : <ChevronRight size={20} aria-hidden="true" />}
            </button>
          )}

          {/* Bottom: reaction bar + filmstrip */}
          <div className={styles.bottom}>
            <div className={styles.reactionBar} role="group" aria-label={t('lightbox.reactions')}>
              {shownSummary.map((s) => (
                <LightboxChip key={s.type} summary={s} onToggle={react} />
              ))}
              <button
                type="button"
                className={styles.iconButton}
                style={{ width: 32, height: 32 }}
                onClick={() => setPickerOpen((o) => !o)}
                aria-label={t('lightbox.addReaction')}
                aria-expanded={pickerOpen}
              >
                <Plus size={16} aria-hidden="true" />
              </button>
              {pickerOpen && (
                <div className={styles.picker} role="menu" aria-label={t('lightbox.addReaction')}>
                  {REACTION_TYPES.map((type) => {
                    const mine = summary.find((s) => s.type === type)?.reactedByMe;
                    return (
                      <button
                        key={type}
                        type="button"
                        role="menuitem"
                        className={[styles.pickerItem, mine ? styles.pickerItemMine : '']
                          .filter(Boolean)
                          .join(' ')}
                        onClick={() => {
                          setPickerOpen(false);
                          void react(type);
                        }}
                        aria-label={t(`reaction.${type}` as never)}
                      >
                        {REACTION_EMOJI[type]}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
            <Filmstrip media={media} index={index} onSelect={goTo} />
          </div>
        </div>
      </div>

      {commentsOpen && (
        <aside className={styles.commentsPanel} aria-label={t('lightbox.comments')}>
          <div className={styles.commentsHead}>
            <span className={styles.commentsTitle}>{t('lightbox.comments')}</span>
            <span className={styles.commentsHint}>{t('lightbox.commentsClose')}</span>
          </div>
          <div className={styles.commentsList}>
            {comments === null ? (
              <span className="t-meta">…</span>
            ) : (
              comments.map((c) => (
                <CommentRow
                  key={c.id}
                  comment={c}
                  mine={c.authorId === viewerId}
                  canDelete={c.authorId === viewerId || isAlbumOwner}
                  editing={editing?.id === c.id ? editing : null}
                  onEditStart={() => setEditing({ id: c.id, body: c.body })}
                  onEditChange={(body) => setEditing({ id: c.id, body })}
                  onEditSave={saveEdit}
                  onEditCancel={() => setEditing(null)}
                  onDelete={() => setDeleteTarget(c)}
                />
              ))
            )}
            {pendingComment && (
              <div className={`${styles.comment} ${styles.commentPending}`}>
                <div className={styles.commentMain}>
                  <div className={styles.commentBody}>{pendingComment.body}</div>
                  {pendingComment.failed && (
                    <div className={styles.commentActions}>
                      <button type="button" className={styles.commentAction} onClick={retryComment}>
                        {t('common.retry')}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
          <div className={styles.composer}>
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder={t('lightbox.addComment')}
              maxLength={MAX_COMMENT_LENGTH}
              rows={2}
              style={{
                width: '100%',
                background: 'var(--bg-sunken)',
                border: '1px solid var(--rule-strong)',
                borderRadius: 'var(--r-sm)',
                padding: '9px 12px',
                fontSize: 13.5,
                color: 'var(--text)',
                resize: 'none',
              }}
              onKeyDown={(e) => {
                if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
                  e.preventDefault();
                  void postComment();
                }
              }}
            />
            <div className={styles.composerRow}>
              <span className={styles.composerHint}>{t('lightbox.postHint')}</span>
              <Button size="sm" onClick={() => void postComment()} disabled={!draft.trim()}>
                {t('lightbox.post')}
              </Button>
            </div>
          </div>
        </aside>
      )}

      <Confirm
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={confirmDelete}
        title={t('lightbox.deleteComment.title')}
        body={t('lightbox.deleteComment.body')}
        confirmLabel={t('lightbox.delete')}
        cancelLabel={t('common.cancel')}
        busy={deleteBusy}
      />
    </div>,
    document.body,
  );
}

function LightboxChip({
  summary,
  onToggle,
}: {
  summary: ReactionSummaryDto;
  onToggle: (type: ReactionType) => void;
}) {
  const { t } = useLocale();
  const [burst, setBurst] = useState(0);
  return (
    <button
      type="button"
      className={styles.iconButton}
      style={{
        width: 'auto',
        height: 32,
        paddingInline: 10,
        borderRadius: 999,
        gap: 5,
        fontSize: 13,
        background: summary.reactedByMe ? 'var(--warm)' : undefined,
        color: summary.reactedByMe ? 'var(--on-warm)' : undefined,
      }}
      onClick={() => {
        if (!summary.reactedByMe) setBurst((b) => b + 1);
        onToggle(summary.type);
      }}
      aria-pressed={summary.reactedByMe}
      aria-label={`${t(`reaction.${summary.type}` as never)} — ${summary.count}`}
    >
      <span
        key={burst}
        style={burst > 0 ? { animation: 'chip-burst 320ms cubic-bezier(.2,1.25,.3,1)' } : undefined}
        aria-hidden="true"
      >
        {REACTION_EMOJI[summary.type]}
      </span>
      <span style={{ fontWeight: 600 }}>{summary.count}</span>
    </button>
  );
}

function Filmstrip({
  media,
  index,
  onSelect,
}: {
  media: MediaDto[];
  index: number;
  onSelect: (i: number) => void;
}) {
  // 5–9 thumbs centred on the current item.
  const range = useMemo(() => {
    const radius = 4;
    const start = Math.max(0, Math.min(index - radius, media.length - (radius * 2 + 1)));
    return media.slice(Math.max(0, start), start + radius * 2 + 1).map((m, i) => ({
      item: m,
      idx: Math.max(0, start) + i,
    }));
  }, [index, media]);

  if (media.length < 2) return null;
  return (
    <div className={styles.filmstrip} aria-hidden="true">
      {range.map(({ item, idx }) => (
        <button
          key={item.id}
          type="button"
          tabIndex={-1}
          className={[styles.filmThumb, idx === index ? styles.filmCurrent : '']
            .filter(Boolean)
            .join(' ')}
          onClick={() => onSelect(idx)}
        >
          <img src={item.thumbnailUrl ?? item.url} alt="" loading="lazy" />
        </button>
      ))}
    </div>
  );
}

function CommentRow({
  comment,
  mine,
  canDelete,
  editing,
  onEditStart,
  onEditChange,
  onEditSave,
  onEditCancel,
  onDelete,
}: {
  comment: CommentDto;
  mine: boolean;
  canDelete: boolean;
  editing: { id: string; body: string } | null;
  onEditStart: () => void;
  onEditChange: (body: string) => void;
  onEditSave: () => void;
  onEditCancel: () => void;
  onDelete: () => void;
}) {
  const { t } = useLocale();
  const rel = relativeTimeParts(comment.createdAt);
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
              : formatDateTime(comment.createdAt);
  const edited = comment.updatedAt !== comment.createdAt;

  return (
    <div className={styles.comment}>
      <Avatar
        userId={comment.authorId}
        displayName={comment.authorDisplayName}
        avatarUrl={comment.authorAvatarUrl}
        size="sm"
        decorative
      />
      <div className={styles.commentMain}>
        <div className={styles.commentAuthor}>
          {comment.authorDisplayName}{' '}
          <span className={styles.commentTime}>
            · {time}
            {edited ? ` · ${t('lightbox.edited')}` : ''}
          </span>
        </div>
        {editing ? (
          <div>
            <textarea
              value={editing.body}
              onChange={(e) => onEditChange(e.target.value)}
              maxLength={MAX_COMMENT_LENGTH}
              rows={2}
              autoFocus
              style={{
                width: '100%',
                background: 'var(--bg-sunken)',
                border: '1px solid var(--rule-strong)',
                borderRadius: 'var(--r-sm)',
                padding: '6px 9px',
                fontSize: 13.5,
                color: 'var(--text)',
                resize: 'none',
                marginTop: 4,
              }}
              onKeyDown={(e) => {
                if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
                  e.preventDefault();
                  onEditSave();
                }
                if (e.key === 'Escape') {
                  e.stopPropagation();
                  onEditCancel();
                }
              }}
            />
            <div className={styles.commentActions}>
              <button type="button" className={styles.commentAction} onClick={onEditSave}>
                {t('common.save')}
              </button>
              <button type="button" className={styles.commentAction} onClick={onEditCancel}>
                {t('common.cancel')}
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className={styles.commentBody}>{comment.body}</div>
            {(mine || canDelete) && (
              <div className={styles.commentActions}>
                {mine && (
                  <button type="button" className={styles.commentAction} onClick={onEditStart}>
                    {t('lightbox.edit')}
                  </button>
                )}
                {canDelete && (
                  <button type="button" className={styles.commentDelete} onClick={onDelete}>
                    {t('lightbox.delete')}
                  </button>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
