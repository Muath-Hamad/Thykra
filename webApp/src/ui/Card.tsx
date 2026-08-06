import type { CSSProperties, ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import type { AlbumDto } from '../api/albums';
import { useLocale } from '../i18n/LocaleProvider';
import { formatMonthYear, relativeTimeParts } from '../lib/format';
import { AvatarStack } from './Avatar';
import { Skeleton } from './Skeleton';
import styles from './Card.module.css';

export function Card({
  flush,
  children,
  className,
  style,
}: {
  flush?: boolean;
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <div
      className={[styles.card, flush ? styles.flush : '', className ?? ''].filter(Boolean).join(' ')}
      style={style}
    >
      {children}
    </div>
  );
}

export interface TripCardActivity {
  actorName: string;
  count: number;
  when: string;
}

export function TripCard({
  album,
  newCount = 0,
  activity,
  photoCount,
}: {
  album: AlbumDto;
  /** Media uploaded since the viewer's last visit — derived client-side. */
  newCount?: number;
  /** "Sara added 12 photos · 2h ago" line, when known. */
  activity?: TripCardActivity | null;
  photoCount?: number;
}) {
  const { t, lang } = useLocale();
  const solo = album.memberCount <= 1;

  let metaLine: ReactNode;
  let metaLive = false;
  if (activity && activity.count > 0) {
    const rel = relativeTimeParts(activity.when);
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
                : formatMonthYear(activity.when, lang);
    metaLine = t('trips.card.activity.added', {
      name: activity.actorName,
      n: activity.count,
      time,
    });
    metaLive = true;
  } else if (solo) {
    metaLine = t('trips.card.justYou');
  } else {
    metaLine = t('trips.card.meta', {
      people: album.memberCount,
      date: formatMonthYear(album.createdAt, lang),
    });
  }

  return (
    <Link
      to="/trips/$albumId"
      params={{ albumId: album.id }}
      className={[styles.tripCard, !album.coverUrl ? styles.dashed : ''].filter(Boolean).join(' ')}
    >
      <div className={styles.cover}>
        {album.coverUrl ? (
          <img src={album.coverUrl} alt="" loading="lazy" />
        ) : (
          <div className={styles.coverEmpty}>
            <span className={styles.coverEmptyLabel}>{t('trips.card.noPhotos')}</span>
          </div>
        )}
        {newCount > 0 && <span className={styles.newBadge}>{t('trips.card.new', { n: newCount })}</span>}
        {album.visibility === 'LINK_SHARED' && (
          <span className={styles.visBadge}>{t('trips.card.linkShared')}</span>
        )}
      </div>
      <div className={styles.body}>
        <div className={styles.title}>{album.title}</div>
        <div className={[styles.meta, metaLive ? styles.metaLive : ''].filter(Boolean).join(' ')}>
          {metaLine}
        </div>
        <div className={styles.footer}>
          <AvatarStack
            people={album.previewMembers}
            totalCount={album.memberCount}
            size="sm"
            ringColor="var(--bg-raised)"
            asDecoration
          />
          <span className={styles.footerEnd}>
            {solo
              ? t('trips.card.owner')
              : photoCount != null
                ? t('common.photos.count', { n: photoCount })
                : null}
          </span>
        </div>
      </div>
    </Link>
  );
}

export function TripCardSkeleton() {
  return (
    <div className={styles.tripCard} aria-hidden="true">
      <Skeleton aspectRatio="16 / 10" radius={0} />
      <div className={styles.body}>
        <Skeleton width="62%" height={16} />
        <div style={{ marginTop: 9 }}>
          <Skeleton width="40%" height={11} />
        </div>
        <div style={{ marginTop: 14 }}>
          <Skeleton width={96} height={26} radius={999} />
        </div>
      </div>
    </div>
  );
}
