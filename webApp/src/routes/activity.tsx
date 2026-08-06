/**
 * /activity — the global feed (Doc 3 § s-activity).
 *
 * Reverse-chronological, grouped by day, the trip named beside the heading
 * when a whole day belongs to one trip. The endpoint is an API addition: if
 * it isn't there yet the page shows the designed "nothing yet" state rather
 * than disappearing, so the IA is stable from day one.
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  getGlobalActivity,
  markActivitySeen as markActivitySeenApi,
} from '../api/activity';
import { useLocale } from '../i18n/LocaleProvider';
import { getActivitySeen, markActivitySeen } from '../lib/lastVisit';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { Delayed, ListSkeleton } from '../ui/Skeleton';
import { Segmented } from '../ui/Tabs';
import { useDocumentTitle } from '../ui/hooks';
import {
  ActivityGroups,
  LoadMoreControl,
  activityStyles as styles,
  fromServerEvent,
  matchesFilter,
  type ActivityFilter,
  type FeedEvent,
} from './activityShared';

type Status = 'loading' | 'ready' | 'unavailable' | 'error';

export function ActivityPage() {
  const { t, lang } = useLocale();
  useDocumentTitle(`${t('nav.activity')} · ${t('app.name')}`);

  const [status, setStatus] = useState<Status>('loading');
  const [events, setEvents] = useState<FeedEvent[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [lastSeen, setLastSeen] = useState<string | null>(null);
  const [paging, setPaging] = useState(false);
  const [pageError, setPageError] = useState(false);
  const [filter, setFilter] = useState<ActivityFilter>('all');
  const seenMarked = useRef(false);

  const loadFirst = useCallback(async () => {
    setStatus('loading');
    setPageError(false);
    const resp = await getGlobalActivity().catch(() => null);
    // A transport failure is an error the reader can retry; an error envelope
    // is the endpoint saying "not here yet" — that reads as empty, not broken.
    if (!resp) {
      setStatus('error');
      return;
    }
    if (!resp.success || !resp.data) {
      setStatus('unavailable');
      return;
    }
    const feed = resp.data;
    setEvents(feed.items.map(fromServerEvent));
    setCursor(feed.nextCursor ?? null);
    setLastSeen(feed.lastSeenAt ?? getActivitySeen());
    setStatus('ready');
    if (!seenMarked.current) {
      seenMarked.current = true;
      // Best effort: the read marker is an API addition, the local stamp is not.
      void markActivitySeenApi(new Date().toISOString()).catch(() => undefined);
      markActivitySeen();
    }
  }, []);

  useEffect(() => {
    void loadFirst();
  }, [loadFirst]);

  const loadMore = useCallback(async () => {
    if (!cursor || paging) return;
    setPaging(true);
    setPageError(false);
    const resp = await getGlobalActivity(cursor).catch(() => null);
    setPaging(false);
    if (!resp?.success || !resp.data) {
      setPageError(true);
      return;
    }
    const feed = resp.data;
    setEvents((prev) => [...prev, ...feed.items.map(fromServerEvent)]);
    setCursor(feed.nextCursor ?? null);
  }, [cursor, paging]);

  const visible = useMemo(
    () => events.filter((event) => matchesFilter(event, filter)),
    [events, filter],
  );

  // Comments and reactions only exist in the server feed — the option is
  // hidden, never disabled, until there is feed data behind it.
  const filterOptions = useMemo(() => {
    const options: { value: ActivityFilter; label: string }[] = [
      { value: 'all', label: t('activity.filter.all') },
      { value: 'photos', label: t('activity.filter.photos') },
      { value: 'people', label: t('activity.filter.people') },
    ];
    if (status === 'ready') {
      options.push({ value: 'comments', label: t('activity.filter.comments') });
    }
    return options;
  }, [status, t]);

  const emptyState = (
    <EmptyState
      title={t('activity.empty.title')}
      body={t('activity.empty.body')}
      actions={
        <Button as="a" href="/trips" variant="secondary">
          {t('activity.empty.cta')}
        </Button>
      }
    />
  );

  return (
    <div className={`${styles.page} page-enter`}>
      <div className={styles.column}>
        <header className={styles.header}>
          <div className="t-label">{t('activity.microLabel')}</div>
          <h1 className={`${styles.headline} t-display-m`}>
            {t('activity.titleBefore')}
            <em className="accent-phrase">{t('activity.titleAccent')}</em>
          </h1>
        </header>

        {status === 'ready' && events.length > 0 && (
          <Segmented
            label={t('activity.microLabel')}
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
                <Button variant="secondary" onClick={() => void loadFirst()}>
                  {t('common.retry')}
                </Button>
              }
            />
          </div>
        )}

        {status === 'unavailable' && <div className={styles.stateArea}>{emptyState}</div>}

        {status === 'ready' &&
          (visible.length === 0 ? (
            <div className={styles.stateArea}>{emptyState}</div>
          ) : (
            <>
              <ActivityGroups
                events={visible}
                scope="global"
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
