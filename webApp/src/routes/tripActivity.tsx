/**
 * /trips/:id/activity — the same row system, trip scope (Doc 3 § s-activity).
 *
 * Ships before the endpoint: when /albums/:id/activity isn't there, the page
 * derives MEDIA_ADDED and MEMBER_JOINED from the trip's media and members.
 * Comments and reactions can't be derived, so that filter is hidden — not
 * disabled — until the feed exists.
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from '@tanstack/react-router';
import { getAlbumActivity } from '../api/activity';
import { getMembers } from '../api/albums';
import { getAlbumMedia } from '../api/media';
import { useLocale } from '../i18n/LocaleProvider';
import { deriveTripActivity } from '../lib/activityDerive';
import { getActivitySeen } from '../lib/lastVisit';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { Delayed, ListSkeleton } from '../ui/Skeleton';
import { Segmented } from '../ui/Tabs';
import { useDocumentTitle } from '../ui/hooks';
import {
  ActivityGroups,
  LoadMoreControl,
  TripTabs,
  activityStyles as styles,
  fromDerivedEvent,
  fromServerEvent,
  matchesFilter,
  type ActivityFilter,
  type FeedEvent,
} from './activityShared';

type Status = 'loading' | 'ready' | 'error';

export function TripActivityPage() {
  const { albumId } = useParams({ strict: false }) as { albumId: string };
  const { t, lang } = useLocale();
  useDocumentTitle(`${t('trip.tab.activity')} · ${t('app.name')}`);

  const [status, setStatus] = useState<Status>('loading');
  const [source, setSource] = useState<'server' | 'derived'>('derived');
  const [events, setEvents] = useState<FeedEvent[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [lastSeen, setLastSeen] = useState<string | null>(null);
  const [paging, setPaging] = useState(false);
  const [pageError, setPageError] = useState(false);
  const [filter, setFilter] = useState<ActivityFilter>('all');

  const load = useCallback(async () => {
    setStatus('loading');
    setPageError(false);

    const feedResp = await getAlbumActivity(albumId).catch(() => null);
    if (feedResp?.success && feedResp.data) {
      const feed = feedResp.data;
      setSource('server');
      setEvents(feed.items.map(fromServerEvent));
      setCursor(feed.nextCursor ?? null);
      setLastSeen(feed.lastSeenAt ?? getActivitySeen());
      setStatus('ready');
      return;
    }

    // No feed yet — derive the two highest-frequency events client-side.
    const [mediaResp, membersResp] = await Promise.all([
      getAlbumMedia(albumId).catch(() => null),
      getMembers(albumId).catch(() => null),
    ]);
    if (!mediaResp?.success || !mediaResp.data) {
      setStatus('error');
      return;
    }
    const members = membersResp?.success && membersResp.data ? membersResp.data : [];
    setSource('derived');
    setEvents(
      deriveTripActivity(mediaResp.data, members).map((event) =>
        fromDerivedEvent(event, albumId, ''),
      ),
    );
    setCursor(null);
    setLastSeen(getActivitySeen());
    setStatus('ready');
  }, [albumId]);

  useEffect(() => {
    void load();
  }, [load]);

  const loadMore = useCallback(async () => {
    if (!cursor || paging) return;
    setPaging(true);
    setPageError(false);
    const resp = await getAlbumActivity(albumId, cursor).catch(() => null);
    setPaging(false);
    if (!resp?.success || !resp.data) {
      setPageError(true);
      return;
    }
    const feed = resp.data;
    setEvents((prev) => [...prev, ...feed.items.map(fromServerEvent)]);
    setCursor(feed.nextCursor ?? null);
  }, [albumId, cursor, paging]);

  const visible = useMemo(
    () => events.filter((event) => matchesFilter(event, filter)),
    [events, filter],
  );

  const filterOptions = useMemo(() => {
    const options: { value: ActivityFilter; label: string }[] = [
      { value: 'all', label: t('activity.filter.all') },
      { value: 'photos', label: t('activity.filter.photos') },
      { value: 'people', label: t('activity.filter.people') },
    ];
    if (source === 'server') {
      options.push({ value: 'comments', label: t('activity.filter.comments') });
    }
    return options;
  }, [source, t]);

  return (
    <div className={`${styles.page} page-enter`}>
      <div className={styles.column}>
        <TripTabs albumId={albumId} current="activity" t={t} />

        <header className={styles.header}>
          <h1 className="t-display-s">{t('trip.tab.activity')}</h1>
        </header>

        {status === 'ready' && events.length > 0 && (
          <Segmented
            label={t('trip.tab.activity')}
            options={filterOptions}
            value={filter}
            onChange={setFilter}
            fullWidthMobile
          />
        )}

        {status === 'loading' && (
          <div className={styles.loading}>
            <Delayed>
              <ListSkeleton rows={3} />
            </Delayed>
          </div>
        )}

        {status === 'error' && (
          <div className={styles.stateArea}>
            <EmptyState
              kind="error"
              title={t('activity.error.title')}
              actions={
                <Button variant="secondary" onClick={() => void load()}>
                  {t('common.retry')}
                </Button>
              }
            />
          </div>
        )}

        {status === 'ready' &&
          (visible.length === 0 ? (
            <div className={styles.stateArea}>
              <EmptyState
                title={t('activity.empty.title')}
                body={t('activity.empty.body')}
                actions={
                  <Button as="a" href={`/trips/${albumId}`} variant="secondary">
                    {t('recaps.backToTrip')}
                  </Button>
                }
              />
            </div>
          ) : (
            <>
              <ActivityGroups
                events={visible}
                scope="trip"
                lastSeen={lastSeen}
                t={t}
                lang={lang}
              />
              {cursor && (
                <LoadMoreControl
                  loading={paging}
                  error={pageError}
                  onLoadMore={() => void loadMore()}
                  t={t}
                />
              )}
            </>
          ))}
      </div>
    </div>
  );
}
