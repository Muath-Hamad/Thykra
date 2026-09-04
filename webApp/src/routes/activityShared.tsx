/**
 * Shared presentation for the Activity + Recaps screen group
 * (Doc 3 § s-activity, § s-recaps).
 *
 * One row system, two scopes: the global feed groups by day and names the
 * trip; the per-trip feed drops the trip label. Both are plainly
 * reverse-chronological — there is no ranking in a six-person album.
 */

import { useEffect, useRef, type ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import type { ActivityEventDto } from '../api/activity';
import { REACTION_EMOJI, type ReactionType } from '../api/reactions';
import type { RecapDto } from '../api/recaps';
import type { DerivedActivityEvent } from '../lib/activityDerive';
import { formatDayShort, formatMonthYear, relativeTimeParts } from '../lib/format';
import type { Lang, StringKey } from '../i18n/LocaleProvider';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Plate } from '../ui/Plate';
import { Skeleton } from '../ui/Skeleton';
import { Stamp } from '../ui/Stamp';
import { Tabs, tabClass } from '../ui/Tabs';
import styles from './activityShared.module.css';

export type TFunc = (key: StringKey, params?: Record<string, string | number>) => string;

export type ActivityFilter = 'all' | 'photos' | 'people' | 'comments';

/** One normalised row, whatever produced it — server feed or client derivation. */
export interface FeedEvent {
  id: string;
  type: 'MEDIA_ADDED' | 'MEMBER_JOINED' | 'COMMENT' | 'REACTION';
  actor: { userId: string; displayName: string; avatarUrl?: string | null };
  albumId: string;
  albumTitle: string;
  createdAt: string;
  count?: number | null;
  mediaIds: string[];
  thumbnailUrls: string[];
  commentBody?: string | null;
  reactionType?: ReactionType | null;
}

export function fromServerEvent(e: ActivityEventDto): FeedEvent {
  return {
    id: e.id,
    type: e.type,
    actor: e.actor,
    albumId: e.album.id,
    albumTitle: e.album.title,
    createdAt: e.createdAt,
    count: e.count,
    mediaIds: e.mediaIds ?? [],
    thumbnailUrls: e.thumbnailUrls ?? [],
    commentBody: e.commentBody,
    reactionType: e.reactionType,
  };
}

export function fromDerivedEvent(
  e: DerivedActivityEvent,
  albumId: string,
  albumTitle: string,
): FeedEvent {
  return {
    id: e.id,
    type: e.type,
    actor: e.actor,
    albumId,
    albumTitle,
    createdAt: e.createdAt,
    count: e.count,
    mediaIds: e.mediaIds ?? [],
    thumbnailUrls: e.thumbnailUrls ?? [],
  };
}

export function matchesFilter(event: FeedEvent, filter: ActivityFilter): boolean {
  switch (filter) {
    case 'photos':
      return event.type === 'MEDIA_ADDED';
    case 'people':
      return event.type === 'MEMBER_JOINED';
    case 'comments':
      return event.type === 'COMMENT' || event.type === 'REACTION';
    default:
      return true;
  }
}

/** Structured relative time, rendered through the time.* strings. */
export function relativeLabel(iso: string, t: TFunc, lang: Lang): string {
  const rel = relativeTimeParts(iso);
  switch (rel.kind) {
    case 'justNow':
      return t('time.justNow');
    case 'minutes':
      return t('time.minutesAgo', { n: rel.n });
    case 'hours':
      return t('time.hoursAgo', { n: rel.n });
    case 'yesterday':
      return t('time.yesterday');
    case 'days':
      return t('time.daysAgo', { n: rel.n });
    default:
      return formatDayShort(rel.iso, lang);
  }
}

// ── Day grouping ───────────────────────────────────────────────────────────

export type DayGroupKey = 'today' | 'yesterday' | 'week' | 'earlier';

const GROUP_LABEL: Record<DayGroupKey, StringKey> = {
  today: 'activity.group.today',
  yesterday: 'activity.group.yesterday',
  week: 'activity.group.week',
  earlier: 'activity.group.earlier',
};

export interface DayGroup {
  key: DayGroupKey;
  events: FeedEvent[];
  /** Set only in the global scope, and only when the whole group is one trip. */
  tripTitle?: string;
}

function calendarDaysAgo(iso: string, now: Date): number {
  const then = new Date(iso);
  const a = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const b = new Date(then.getFullYear(), then.getMonth(), then.getDate()).getTime();
  return Math.round((a - b) / 86_400_000);
}

function groupKeyFor(iso: string, now: Date): DayGroupKey {
  const days = calendarDaysAgo(iso, now);
  if (days <= 0) return 'today';
  if (days === 1) return 'yesterday';
  if (days < 7) return 'week';
  return 'earlier';
}

export function groupByDay(
  events: FeedEvent[],
  scope: 'global' | 'trip',
  now: Date = new Date(),
): DayGroup[] {
  const groups: DayGroup[] = [];
  for (const event of events) {
    const key = groupKeyFor(event.createdAt, now);
    const last = groups[groups.length - 1];
    if (last && last.key === key) last.events.push(event);
    else groups.push({ key, events: [event] });
  }
  if (scope === 'global') {
    for (const group of groups) {
      const first = group.events[0];
      const uniform = group.events.every((e) => e.albumId === first.albumId);
      if (uniform && first.albumTitle) group.tripTitle = first.albumTitle;
    }
  }
  return groups;
}

// ── The row ────────────────────────────────────────────────────────────────

/** Interpolation slot — keeps the name in the sentence order the language wants. */
const NAME_SLOT = '\u0001';

function withActorName(template: string, name: string): ReactNode[] {
  const parts = template.split(NAME_SLOT);
  const out: ReactNode[] = [];
  parts.forEach((part, i) => {
    if (i > 0) {
      out.push(
        <strong key={`n${i}`} className={styles.actor}>
          {name}
        </strong>,
      );
    }
    if (part) out.push(<span key={`t${i}`}>{part}</span>);
  });
  return out;
}

function sentenceFor(event: FeedEvent, withTrip: boolean, t: TFunc): ReactNode[] {
  const name = NAME_SLOT;
  const trip = event.albumTitle;
  switch (event.type) {
    case 'MEDIA_ADDED': {
      const n = event.count ?? event.mediaIds.length ?? 1;
      const things = n === 1 ? t('common.photo.one') : t('common.photos.count', { n });
      return withActorName(
        withTrip
          ? t('activity.row.added.toTrip', { name, things, trip })
          : t('activity.row.added', { name, things }),
        event.actor.displayName,
      );
    }
    case 'MEMBER_JOINED':
      return withActorName(
        withTrip ? t('activity.row.joined.trip', { name, trip }) : t('activity.row.joined', { name }),
        event.actor.displayName,
      );
    case 'COMMENT':
      return withActorName(
        t('activity.row.commented', { name, body: event.commentBody ?? '' }),
        event.actor.displayName,
      );
    case 'REACTION': {
      const emoji = event.reactionType ? REACTION_EMOJI[event.reactionType] : '';
      const n = event.count ?? event.mediaIds.length ?? 1;
      return withActorName(
        n > 1
          ? t('activity.row.reacted.many', { name, emoji, n })
          : t('activity.row.reacted', { name, emoji }),
        event.actor.displayName,
      );
    }
  }
}

function ThumbCluster({ event }: { event: FeedEvent }) {
  const thumbs = event.thumbnailUrls.slice(0, 3);
  if (thumbs.length === 0) return null;
  const total = Math.max(event.count ?? 0, event.mediaIds.length, thumbs.length);
  const wideOverflow = total - Math.min(thumbs.length, 3);
  const narrowOverflow = total - Math.min(thumbs.length, 2);
  return (
    <span className={styles.thumbs} aria-hidden="true">
      {thumbs.map((url, i) => (
        <img
          key={`${url}-${i}`}
          src={url}
          alt=""
          className={[styles.thumb, i === 2 ? styles.thumbThird : ''].filter(Boolean).join(' ')}
        />
      ))}
      {wideOverflow > 0 && <span className={`${styles.chip} ${styles.chipWide}`}>+{wideOverflow}</span>}
      {narrowOverflow > 0 && (
        <span className={`${styles.chip} ${styles.chipNarrow}`}>+{narrowOverflow}</span>
      )}
    </span>
  );
}

export function ActivityRow({
  event,
  withTrip,
  unseen,
  t,
  lang,
}: {
  event: FeedEvent;
  withTrip: boolean;
  unseen: boolean;
  t: TFunc;
  lang: Lang;
}) {
  // MEMBER_JOINED opens the trip itself; everything else opens its first plate.
  const mediaId = event.type === 'MEMBER_JOINED' ? undefined : event.mediaIds[0];
  return (
    <Link
      to="/trips/$albumId"
      params={{ albumId: event.albumId }}
      search={{ m: mediaId }}
      className={styles.row}
    >
      {unseen && (
        <>
          <span className={styles.dot} aria-hidden="true" />
          <span className="sr-only">{t('activity.unseen')} </span>
        </>
      )}
      <Avatar
        userId={event.actor.userId}
        displayName={event.actor.displayName}
        avatarUrl={event.actor.avatarUrl}
        size="md"
        decorative
      />
      <span className={styles.rowBody}>
        <span className={`${styles.sentence} t-body`}>{sentenceFor(event, withTrip, t)}</span>
        <span className={`${styles.rowMeta} t-meta`}>{relativeLabel(event.createdAt, t, lang)}</span>
      </span>
      <ThumbCluster event={event} />
    </Link>
  );
}

export function ActivityGroups({
  events,
  scope,
  lastSeen,
  t,
  lang,
}: {
  events: FeedEvent[];
  scope: 'global' | 'trip';
  lastSeen: string | null;
  t: TFunc;
  lang: Lang;
}) {
  const groups = groupByDay(events, scope);
  const seenAt = lastSeen ? Date.parse(lastSeen) : null;
  return (
    <div className={styles.list}>
      {groups.map((group, gi) => (
        <section key={`${group.key}-${gi}`} className={styles.group}>
          <h2 className={styles.groupHead}>
            <span className={styles.groupTitle}>{t(GROUP_LABEL[group.key])}</span>
            {group.tripTitle && <span className="t-meta">{group.tripTitle}</span>}
          </h2>
          <div className={styles.rows}>
            {group.events.map((event) => (
              <ActivityRow
                key={event.id}
                event={event}
                withTrip={scope === 'global' && !group.tripTitle}
                unseen={seenAt !== null && Date.parse(event.createdAt) > seenAt}
                t={t}
                lang={lang}
              />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

// ── Paging — the button is always in the DOM; the sentinel clicks it ───────

export function LoadMoreControl({
  loading,
  error,
  onLoadMore,
  t,
}: {
  loading: boolean;
  error: boolean;
  onLoadMore: () => void;
  t: TFunc;
}) {
  const buttonRef = useRef<HTMLButtonElement | HTMLAnchorElement | null>(null);
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el || error) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) buttonRef.current?.click();
      },
      { rootMargin: '240px' },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [error, loading]);

  if (error) {
    // One inline row; everything already loaded stays exactly where it is.
    return (
      <div className={`${styles.pagerError} t-body-s`} role="status">
        <span>{t('trip.loadMoreError')}</span>
        <Button variant="secondary" size="sm" onClick={onLoadMore}>
          {t('common.retry')}
        </Button>
      </div>
    );
  }

  return (
    <div className={styles.pager}>
      <Button ref={buttonRef} variant="secondary" loading={loading} onClick={onLoadMore}>
        {t('common.loadMore')}
      </Button>
      <div ref={sentinelRef} className={styles.sentinel} aria-hidden="true" />
    </div>
  );
}

// ── Per-trip tab row — Trip · Activity · Recaps · Settings ────────────────

export function TripTabs({
  albumId,
  current,
  t,
}: {
  albumId: string;
  current: 'activity' | 'recaps';
  t: TFunc;
}) {
  return (
    <div className={styles.tabsRow}>
      <Tabs label={t('nav.menu')}>
        {/* Exact, or the parent route reads as active on every child tab. */}
        <Link
          to="/trips/$albumId"
          params={{ albumId }}
          search={{}}
          activeOptions={{ exact: true }}
          className={tabClass}
        >
          {t('trip.tab.trip')}
        </Link>
        <Link
          to="/trips/$albumId/activity"
          params={{ albumId }}
          className={tabClass}
          aria-current={current === 'activity' ? 'page' : undefined}
        >
          {t('trip.tab.activity')}
        </Link>
        <Link
          to="/trips/$albumId/recaps"
          params={{ albumId }}
          className={tabClass}
          aria-current={current === 'recaps' ? 'page' : undefined}
        >
          {t('trip.tab.recaps')}
        </Link>
        <Link to="/trips/$albumId/settings" params={{ albumId }} className={tabClass}>
          {t('trip.tab.settings')}
        </Link>
      </Tabs>
    </div>
  );
}

// ── Recap cards ───────────────────────────────────────────────────────────

export function RecapCard({
  recap,
  t,
  lang,
  onShare,
}: {
  recap: RecapDto;
  t: TFunc;
  lang: Lang;
  onShare: (recap: RecapDto) => void;
}) {
  const stampName = formatMonthYear(recap.periodStart ?? recap.createdAt, lang);
  return (
    <article className={styles.recapCard}>
      <div className={styles.recapCover}>
        {recap.coverUrl ? (
          <Plate src={recap.coverUrl} alt="" aspectRatio="21 / 9" />
        ) : (
          <div className={styles.recapCoverBlank} aria-hidden="true" />
        )}
        <span className={styles.recapStamp}>
          <Stamp tone="ink" eyebrow={t('recaps.edition')} name={stampName} />
        </span>
      </div>
      <div className={styles.recapBody}>
        <h3 className="t-display-s">{recap.title}</h3>
        {/* recaps.chosen needs a total and a people count the list payload does
            not carry, so the hub states only what it knows. */}
        <p className={`${styles.recapMeta} t-meta`}>
          {t('recaps.meta', { n: recap.mediaIds.length, title: recap.albumTitle })}
        </p>
        <div className={styles.recapActions}>
          {recap.shareToken && (
            <>
              <Button as="a" href={`/r/${recap.shareToken}`} size="sm">
                {t('common.open')}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => onShare(recap)}>
                {t('common.share')}
              </Button>
            </>
          )}
        </div>
      </div>
    </article>
  );
}

export function RecapBuildingRow({ recap, t }: { recap: RecapDto; t: TFunc }) {
  return (
    <div className={styles.recapRow} role="status">
      <Skeleton className={styles.buildingSquare} radius={0} />
      <div className={styles.recapRowText}>
        <div className={styles.recapRowTitle}>{recap.albumTitle}</div>
        <p className="t-meta">{t('recaps.building')}</p>
      </div>
    </div>
  );
}

export function RecapFailedRow({
  recap,
  t,
  busy,
  onRetry,
}: {
  recap: RecapDto;
  t: TFunc;
  busy: boolean;
  onRetry: (recap: RecapDto) => void;
}) {
  return (
    <div className={`${styles.recapRow} ${styles.recapRowBad}`} role="status">
      <div className={styles.recapRowText}>
        <div className={styles.recapRowTitle}>{recap.albumTitle}</div>
        <p className="t-meta">{t('recaps.failed')}</p>
      </div>
      <Button variant="secondary" size="sm" loading={busy} onClick={() => onRetry(recap)}>
        {t('common.retry')}
      </Button>
    </div>
  );
}

export const activityStyles = styles;
