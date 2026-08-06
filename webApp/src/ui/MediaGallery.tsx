import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  type KeyboardEvent,
} from 'react';
import { useLocale } from '../i18n/LocaleProvider';
import {
  groupIntoChapters,
  formatOrdinal,
  type Chapter,
  type ChapterMedia,
} from '../lib/chapters';
import {
  layoutJustifiedRows,
  ratioOf,
  rowGap,
  targetRowHeight,
} from '../lib/justified';
import { formatDayShort, formatDateMedium, formatDuration } from '../lib/format';
import { Plate } from './Plate';
import { useContainerWidth, useViewportWidth } from './hooks';
import styles from './MediaGallery.module.css';

export interface GalleryMedia extends ChapterMedia {
  url: string;
  thumbnailUrl?: string | null;
  width?: number | null;
  height?: number | null;
  durationMs?: number | null;
}

export type GalleryView = 'chapters' | 'contact';

export interface MediaGalleryProps<T extends GalleryMedia> {
  media: T[];
  view?: GalleryView;
  onOpen: (item: T) => void;
  /** Corner chips per media id: ["❤️ 4", "💬 2"]. */
  chipsFor?: (item: T) => string[] | undefined;
  /** Accessible name per plate: "Photo by Sara, Sat 12 April 17:42, …". */
  labelFor: (item: T) => string;
  /** Focus-return registry — the lightbox close hands focus back here. */
  registerItem?: (id: string, el: HTMLButtonElement | null) => void;
  /** Album key for the once-per-session chapter stagger. */
  animKey?: string;
  /** Sticky offset for chapter headers (px below the top bar). */
  stickyTop?: number;
}

// Chapters that already played their entrance this session.
const staggeredChapters = new Set<string>();

interface FlatEntry<T> {
  item: T;
  chapterIdx: number;
  /** Row identity within the whole gallery for ↑/↓ targeting. */
  rowId: number;
  centerX: number;
}

export function MediaGallery<T extends GalleryMedia>({
  media,
  view = 'chapters',
  onOpen,
  chipsFor,
  labelFor,
  registerItem,
  animKey = '',
  stickyTop,
}: MediaGalleryProps<T>) {
  const { lang } = useLocale();
  const arabic = lang === 'ar';
  const [containerRef, containerWidth] = useContainerWidth<HTMLDivElement>();
  const viewportWidth = useViewportWidth();

  const chapters = useMemo(() => groupIntoChapters(media), [media]);

  const target = targetRowHeight(viewportWidth);
  const gap = rowGap(viewportWidth);

  // Build layout rows per chapter, plus a flat keyboard model.
  const { chapterRows, flat, order } = useMemo(() => {
    const chapterRows = new Map<
      string,
      { items: { item: T; width: number }[]; height: number; justified: boolean }[]
    >();
    const flat: FlatEntry<T>[] = [];
    const order: T[] = [];
    let rowId = 0;

    if (view === 'contact') {
      const cols =
        viewportWidth >= 1024 ? 5 : viewportWidth >= 768 ? 4 : viewportWidth >= 480 ? 3 : 2;
      const all = chapters.flatMap((c) => c.items);
      all.forEach((item, i) => {
        const col = i % cols;
        flat.push({
          item,
          chapterIdx: 0,
          rowId: Math.floor(i / cols),
          centerX: col + 0.5,
        });
        order.push(item);
      });
      return { chapterRows, flat, order };
    }

    chapters.forEach((chapter, chapterIdx) => {
      // Lead plate is its own row.
      flat.push({ item: chapter.lead, chapterIdx, rowId: rowId, centerX: containerWidth / 2 });
      order.push(chapter.lead);
      rowId += 1;

      const rows = layoutJustifiedRows(
        chapter.rest.map((item) => ({ item, ratio: ratioOf(item.width, item.height) })),
        containerWidth || 1200,
        target,
        gap,
      );
      chapterRows.set(chapter.key, rows);
      rows.forEach((row) => {
        let x = 0;
        row.items.forEach(({ item, width }) => {
          flat.push({ item, chapterIdx, rowId, centerX: x + width / 2 });
          x += width + gap;
          order.push(item);
        });
        rowId += 1;
      });
    });
    return { chapterRows, flat, order };
  }, [chapters, containerWidth, gap, target, view, viewportWidth]);

  // ── Roving tabindex: the gallery is one tab stop ──
  const focusIdxRef = useRef(0);
  const buttonRefs = useRef(new Map<string, HTMLButtonElement>());

  useEffect(() => {
    if (focusIdxRef.current >= flat.length) focusIdxRef.current = 0;
  }, [flat.length]);

  const setButtonRef = useCallback(
    (id: string) => (el: HTMLButtonElement | null) => {
      if (el) buttonRefs.current.set(id, el);
      else buttonRefs.current.delete(id);
      registerItem?.(id, el);
    },
    [registerItem],
  );

  const focusEntry = (idx: number) => {
    const entry = flat[idx];
    if (!entry) return;
    focusIdxRef.current = idx;
    const el = buttonRefs.current.get(entry.item.id);
    // Make the newly-focused item the roving stop.
    buttonRefs.current.forEach((btn, id) => {
      btn.tabIndex = id === entry.item.id ? 0 : -1;
    });
    el?.focus();
    el?.scrollIntoView({ block: 'nearest' });
  };

  const nearestInRow = (rowId: number, centerX: number): number => {
    let best = -1;
    let bestDist = Infinity;
    flat.forEach((e, i) => {
      if (e.rowId !== rowId) return;
      const d = Math.abs(e.centerX - centerX);
      if (d < bestDist) {
        bestDist = d;
        best = i;
      }
    });
    return best;
  };

  const onKeyDown = (e: KeyboardEvent) => {
    const idx = flat.findIndex((f) => f.item.id === (document.activeElement as HTMLElement)?.dataset?.mediaId);
    const current = idx >= 0 ? idx : focusIdxRef.current;
    const entry = flat[current];
    if (!entry) return;
    const isRtl = document.documentElement.dir === 'rtl';
    const forwardKey = isRtl ? 'ArrowLeft' : 'ArrowRight';
    const backKey = isRtl ? 'ArrowRight' : 'ArrowLeft';

    switch (e.key) {
      case forwardKey:
        e.preventDefault();
        if (current + 1 < flat.length) focusEntry(current + 1);
        break;
      case backKey:
        e.preventDefault();
        if (current > 0) focusEntry(current - 1);
        break;
      case 'ArrowDown': {
        e.preventDefault();
        const next = nearestInRow(entry.rowId + 1, entry.centerX);
        if (next >= 0) focusEntry(next);
        break;
      }
      case 'ArrowUp': {
        e.preventDefault();
        const prev = nearestInRow(entry.rowId - 1, entry.centerX);
        if (prev >= 0) focusEntry(prev);
        break;
      }
      case 'Home': {
        e.preventDefault();
        const first = flat.findIndex((f) => f.chapterIdx === entry.chapterIdx);
        if (first >= 0) focusEntry(first);
        break;
      }
      case 'End': {
        e.preventDefault();
        let last = -1;
        flat.forEach((f, i) => {
          if (f.chapterIdx === entry.chapterIdx) last = i;
        });
        if (last >= 0) focusEntry(last);
        break;
      }
      case 'PageDown': {
        // No next chapter — leave the key to the browser rather than eat the scroll.
        const next = flat.findIndex((f) => f.chapterIdx === entry.chapterIdx + 1);
        if (next < 0) return;
        e.preventDefault();
        focusEntry(next);
        break;
      }
      case 'PageUp': {
        const prev = flat.findIndex((f) => f.chapterIdx === entry.chapterIdx - 1);
        if (prev < 0) return;
        e.preventDefault();
        focusEntry(prev);
        break;
      }
      default:
        return;
    }
  };

  // ── Chapter entrance stagger — once per session, via IntersectionObserver ──
  const chapterEls = useRef(new Map<string, HTMLElement>());
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const key = (entry.target as HTMLElement).dataset.chapterKey!;
          const full = `${animKey}:${key}`;
          if (entry.isIntersecting && !staggeredChapters.has(full)) {
            staggeredChapters.add(full);
            entry.target.classList.add(styles.stagger);
          }
        }
      },
      { threshold: 0.05 },
    );
    chapterEls.current.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
    // `view` matters: the chapter sections only exist in the chapters view, so
    // the observer has to re-attach when the view swaps them in.
  }, [animKey, chapters.length, view]);

  // ── Sticky pin sensing per chapter header ──
  const sentinelEls = useRef(new Map<string, HTMLElement>());
  useEffect(() => {
    if (view !== 'chapters') return;
    const top = stickyTop ?? 56;
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const key = (entry.target as HTMLElement).dataset.sentinelFor!;
          const header = chapterEls.current
            .get(key)
            ?.querySelector<HTMLElement>(`.${styles.header}`);
          header?.setAttribute('data-pinned', String(!entry.isIntersecting));
        }
      },
      { rootMargin: `-${top + 1}px 0px 0px 0px`, threshold: 0 },
    );
    sentinelEls.current.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [chapters.length, stickyTop, view]);

  const firstId = order[0]?.id;

  const renderPlate = (item: T, opts: { width?: number; height?: number; lead?: boolean }) => (
    <Plate
      key={item.id}
      src={item.thumbnailUrl ?? item.url}
      alt={labelFor(item)}
      ariaLabel={labelFor(item)}
      isVideo={item.type === 'VIDEO'}
      durationLabel={item.durationMs ? formatDuration(item.durationMs) : undefined}
      chips={chipsFor?.(item)}
      onClick={() => {
        const idx = flat.findIndex((f) => f.item.id === item.id);
        if (idx >= 0) focusIdxRef.current = idx;
        onOpen(item);
      }}
      tabIndex={item.id === firstId ? 0 : -1}
      buttonRef={(el) => {
        if (el) el.dataset.mediaId = item.id;
        setButtonRef(item.id)(el);
      }}
      eager={opts.lead}
      className={opts.lead ? styles.leadPlate : undefined}
      style={
        opts.lead
          ? { aspectRatio: `${ratioOf(item.width, item.height)}` }
          : opts.width
            ? { width: opts.width, height: opts.height, flex: 'none' }
            : undefined
      }
    />
  );

  if (view === 'contact') {
    const all = chapters.flatMap((c) => c.items);
    return (
      <div
        ref={containerRef}
        className={styles.contact}
        role="grid"
        aria-label="Media"
        onKeyDown={onKeyDown}
      >
        {all.map((item) => (
          <div key={item.id} role="gridcell" className={styles.cell}>
            {renderPlate(item, {})}
          </div>
        ))}
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={styles.gallery}
      role="grid"
      aria-label="Media by day"
      onKeyDown={onKeyDown}
      style={
        {
          '--row-gap': `${gap}px`,
          '--sticky-top': stickyTop != null ? `${stickyTop}px` : undefined,
        } as React.CSSProperties
      }
    >
      {chapters.map((chapter, chapterIdx) => (
        <section
          key={chapter.key}
          className={styles.chapter}
          data-chapter-key={chapter.key}
          ref={(el) => {
            if (el) chapterEls.current.set(chapter.key, el);
            else chapterEls.current.delete(chapter.key);
          }}
          aria-label={chapterLabel(chapter, lang)}
        >
          <div
            data-sentinel-for={chapter.key}
            ref={(el) => {
              if (el) sentinelEls.current.set(chapter.key, el);
              else sentinelEls.current.delete(chapter.key);
            }}
            style={{ height: 1 }}
            aria-hidden="true"
          />
          <ChapterHeaderRow chapter={chapter} arabic={arabic} />
          <div className={styles.lead} role="row" style={{ '--row-index': 0 } as React.CSSProperties}>
            <div role="gridcell">{renderPlate(chapter.lead, { lead: true })}</div>
          </div>
          <div className={styles.rows}>
            {(chapterRows.get(chapter.key) ?? []).map((row, rowIdx) => (
              <div
                key={rowIdx}
                role="row"
                className={styles.row}
                style={
                  {
                    '--row-index': rowIdx + 1,
                    justifyContent: row.justified ? undefined : 'flex-start',
                  } as React.CSSProperties
                }
              >
                {row.items.map(({ item, width }) => (
                  <div key={item.id} role="gridcell" style={{ width, flex: 'none' }}>
                    {renderPlate(item, { width, height: row.height })}
                  </div>
                ))}
              </div>
            ))}
          </div>
          {chapterIdx === chapters.length - 1 ? null : null}
        </section>
      ))}
    </div>
  );
}

function chapterLabel(chapter: Chapter, lang: 'en' | 'ar'): string {
  return chapter.dated
    ? formatDayShort(chapter.date, lang)
    : formatDateMedium(`${chapter.date}T12:00:00`, lang);
}

function ChapterHeaderRow({ chapter, arabic }: { chapter: Chapter; arabic: boolean }) {
  const { t, lang } = useLocale();
  const counts = [
    chapter.photoCount > 0
      ? chapter.photoCount === 1
        ? t('common.photo.one')
        : t('common.photos.count', { n: chapter.photoCount })
      : null,
    chapter.videoCount > 0
      ? chapter.videoCount === 1
        ? t('common.video.one')
        : t('common.videos.count', { n: chapter.videoCount })
      : null,
  ]
    .filter(Boolean)
    .join(' · ');

  return (
    // The chapter header is a heading — screen-reader users jump by day
    // with heading navigation, the accessible equivalent of the sticky rule.
    <h2 className={styles.header} data-pinned="false">
      {chapter.ordinal !== null && (
        <span className={styles.numeral} aria-hidden="true">
          {formatOrdinal(chapter.ordinal, arabic)}
        </span>
      )}
      <span className={styles.headerText}>
        <span className={styles.dayLabel}>
          {chapter.dated ? t('common.day') : ''}
        </span>
        <span className={styles.dayDate}>
          {chapter.dated
            ? formatDayShort(chapter.date, lang)
            : t('trip.chapter.added', { date: formatDateMedium(`${chapter.date}T12:00:00`, lang) })}
        </span>
      </span>
      <span className={styles.counts}>{counts}</span>
    </h2>
  );
}
