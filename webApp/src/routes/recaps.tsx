/**
 * /recaps — the editions hub (Doc 3 § s-recaps).
 *
 * A Recap is a generated edition of a trip: a cover, a title, a handful of
 * chosen plates, shareable as its own link. READY editions are cards with the
 * stamp; BUILDING is a shimmering row that polls; FAILED turns the row and
 * offers a rebuild.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getMyRecaps,
  parseRecapGate,
  requestRecap,
  type RecapDto,
} from '../api/recaps';
import { useLocale } from '../i18n/LocaleProvider';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { Delayed, ListSkeleton } from '../ui/Skeleton';
import { useToast } from '../ui/Toast';
import { useDocumentTitle } from '../ui/hooks';
import {
  RecapBuildingRow,
  RecapCard,
  RecapFailedRow,
  activityStyles as styles,
} from './activityShared';

/** Polling gives up after two minutes — an honest failure beats a spinner. */
const GIVE_UP_MS = 120_000;
const POLL_MS = 5_000;

export function RecapsPage() {
  const { t, lang } = useLocale();
  const { toast } = useToast();
  useDocumentTitle(`${t('nav.recaps')} · ${t('app.name')}`);

  const [recaps, setRecaps] = useState<RecapDto[] | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [gaveUp, setGaveUp] = useState(false);
  const [retrying, setRetrying] = useState<string | null>(null);
  const pollStart = useRef<number | null>(null);

  const load = useCallback(async () => {
    setStatus('loading');
    const resp = await getMyRecaps().catch(() => null);
    if (resp?.success && resp.data) {
      setRecaps(resp.data);
      setStatus('ready');
    } else {
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // Poll while anything is building; stop at two minutes and treat it as failed.
  useEffect(() => {
    if (!recaps) return;
    const building = recaps.some((r) => r.status === 'BUILDING');
    if (!building) {
      pollStart.current = null;
      setGaveUp(false);
      return;
    }
    if (gaveUp) return;
    if (pollStart.current === null) pollStart.current = Date.now();
    const handle = window.setInterval(() => {
      if (pollStart.current !== null && Date.now() - pollStart.current > GIVE_UP_MS) {
        setGaveUp(true);
        return;
      }
      void getMyRecaps().then(
        (resp) => {
          if (resp.success && resp.data) setRecaps(resp.data);
        },
        () => undefined,
      );
    }, POLL_MS);
    return () => window.clearInterval(handle);
  }, [recaps, gaveUp]);

  const share = useCallback(
    async (recap: RecapDto) => {
      const url = `${window.location.origin}/r/${recap.shareToken}`;
      try {
        await navigator.clipboard.writeText(url);
        toast({ kind: 'success', title: t('recaps.reader.copied') });
      } catch {
        toast({ kind: 'error', title: t('common.errorGeneric') });
      }
    },
    [t, toast],
  );

  const rebuild = useCallback(
    async (recap: RecapDto) => {
      setRetrying(recap.id);
      const resp = await requestRecap(recap.albumId).catch(() => null);
      setRetrying(null);
      if (resp?.success) {
        pollStart.current = null;
        setGaveUp(false);
        void load();
        return;
      }
      const needed = parseRecapGate(resp?.error);
      toast(
        needed
          ? { kind: 'warm', title: t('recaps.gate', { n: needed }) }
          : { kind: 'error', title: t('common.errorGeneric') },
      );
    },
    [load, t, toast],
  );

  const effectiveStatus = (recap: RecapDto) =>
    gaveUp && recap.status === 'BUILDING' ? 'FAILED' : recap.status;

  return (
    <div className={`${styles.page} page-enter`}>
      <div className={styles.column}>
        <header className={styles.header}>
          <div className="t-label">{t('recaps.microLabel')}</div>
          <h1 className={`${styles.headline} t-display-m`}>
            {t('recaps.titleBefore')}
            <em className="accent-phrase">{t('recaps.titleAccent')}</em>
          </h1>
        </header>

        {status === 'loading' && (
          <div className={styles.loading}>
            <Delayed>
              <ListSkeleton rows={2} />
            </Delayed>
          </div>
        )}

        {status === 'error' && (
          <div className={styles.stateArea}>
            <EmptyState
              kind="error"
              title={t('recaps.error.title')}
              actions={
                <Button variant="secondary" onClick={() => void load()}>
                  {t('common.retry')}
                </Button>
              }
            />
          </div>
        )}

        {status === 'ready' && recaps && recaps.length === 0 && (
          <div className={styles.stateArea}>
            <EmptyState
              title={t('recaps.empty.title')}
              body={t('recaps.empty.body')}
              actions={
                <Button as="a" href="/trips" variant="secondary">
                  {t('activity.empty.cta')}
                </Button>
              }
            />
          </div>
        )}

        {status === 'ready' && recaps && recaps.length > 0 && (
          <div className={styles.recapList}>
            {recaps.map((recap) => {
              const state = effectiveStatus(recap);
              if (state === 'BUILDING') {
                return <RecapBuildingRow key={recap.id} recap={recap} t={t} />;
              }
              if (state === 'FAILED') {
                return (
                  <RecapFailedRow
                    key={recap.id}
                    recap={recap}
                    t={t}
                    busy={retrying === recap.id}
                    onRetry={(r) => void rebuild(r)}
                  />
                );
              }
              return (
                <RecapCard
                  key={recap.id}
                  recap={recap}
                  t={t}
                  lang={lang}
                  onShare={(r) => void share(r)}
                />
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
