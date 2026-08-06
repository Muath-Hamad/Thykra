/**
 * /trips/:id/recaps — this trip's editions (Doc 3 § s-recaps).
 *
 * Three states, in order of how often they'll be seen at launch: the
 * pre-launch "coming" panel (fewer than ~12 photos), the make panel (enough
 * material), and the cards once editions exist. Nothing is disabled or greyed.
 */

import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { getAlbumMedia } from '../api/media';
import {
  getAlbumRecaps,
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
  TripTabs,
  activityStyles as shared,
} from './activityShared';
import styles from './tripRecaps.module.css';

/** The honest gate — under this many photos there is nothing to choose from. */
const ENOUGH_MEDIA = 12;

export function TripRecapsPage() {
  const { albumId } = useParams({ strict: false }) as { albumId: string };
  const { t, lang } = useLocale();
  const { toast } = useToast();
  useDocumentTitle(`${t('trip.tab.recaps')} · ${t('app.name')}`);

  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [recaps, setRecaps] = useState<RecapDto[]>([]);
  const [mediaCount, setMediaCount] = useState(0);
  const [making, setMaking] = useState(false);
  const [retrying, setRetrying] = useState<string | null>(null);
  const [gate, setGate] = useState<number | null>(null);

  const load = useCallback(async () => {
    setStatus('loading');
    const [recapsResp, mediaResp] = await Promise.all([
      getAlbumRecaps(albumId).catch(() => null),
      getAlbumMedia(albumId).catch(() => null),
    ]);
    // No recaps endpoint yet is the pre-launch state, not an error.
    setRecaps(recapsResp?.success && recapsResp.data ? recapsResp.data : []);
    if (mediaResp?.success && mediaResp.data) {
      setMediaCount(mediaResp.data.length);
      setStatus('ready');
    } else {
      setStatus('error');
    }
  }, [albumId]);

  useEffect(() => {
    void load();
  }, [load]);

  const make = useCallback(async () => {
    setMaking(true);
    setGate(null);
    const resp = await requestRecap(albumId).catch(() => null);
    setMaking(false);
    if (resp?.success) {
      void load();
      return;
    }
    const needed = parseRecapGate(resp?.error);
    if (needed) setGate(needed);
    else toast({ kind: 'error', title: t('common.errorGeneric') });
  }, [albumId, load, t, toast]);

  const rebuild = useCallback(
    async (recap: RecapDto) => {
      setRetrying(recap.id);
      const resp = await requestRecap(recap.albumId).catch(() => null);
      setRetrying(null);
      if (resp?.success) {
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

  return (
    <div className={`${shared.page} page-enter`}>
      <div className={shared.column}>
        <TripTabs albumId={albumId} current="recaps" t={t} />

        {status === 'loading' && (
          <div className={shared.loading}>
            <Delayed>
              <ListSkeleton rows={2} />
            </Delayed>
          </div>
        )}

        {status === 'error' && (
          <div className={shared.stateArea}>
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

        {status === 'ready' && recaps.length > 0 && (
          <header className={shared.header}>
            <h1 className="t-display-s">{t('trip.tab.recaps')}</h1>
          </header>
        )}

        {status === 'ready' && recaps.length > 0 && (
          <div className={shared.recapList}>
            {recaps.map((recap) => {
              if (recap.status === 'BUILDING') {
                return <RecapBuildingRow key={recap.id} recap={recap} t={t} />;
              }
              if (recap.status === 'FAILED') {
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

        {status === 'ready' && recaps.length === 0 && mediaCount >= ENOUGH_MEDIA && (
          <div className={styles.panel}>
            <h1 className="t-display-s">{t('recaps.empty.title')}</h1>
            <p className={`${styles.panelBody} t-body`}>{t('recaps.empty.body')}</p>
            <div className={styles.panelActions}>
              <Button
                variant="warm"
                loading={making}
                loadingLabel={t('recaps.making')}
                onClick={() => void make()}
              >
                {t('recaps.make')}
              </Button>
            </div>
            {gate !== null && (
              <p className={`${styles.gate} t-body-s`} role="status">
                {t('recaps.gate', { n: gate })}
              </p>
            )}
          </div>
        )}

        {status === 'ready' && recaps.length === 0 && mediaCount < ENOUGH_MEDIA && (
          <div className={styles.coming}>
            <div className={styles.comingPlates} aria-hidden="true">
              <span className={`${styles.comingPlate} ${styles.comingPlateBack}`} />
              <span className={`${styles.comingPlate} ${styles.comingPlateFront}`}>
                <span className="t-label">{t('recaps.coming.soon')}</span>
              </span>
            </div>
            <h1 className={`${styles.comingTitle} t-display-s`}>{t('recaps.coming.title')}</h1>
            <p className={`${styles.comingBody} t-body`}>{t('recaps.coming.body')}</p>
            <div className={styles.comingActions}>
              <Button
                variant="secondary"
                onClick={() => toast({ kind: 'success', title: t('recaps.notified') })}
              >
                {t('recaps.notify')}
              </Button>
              <Link
                to="/trips/$albumId"
                params={{ albumId }}
                search={{}}
                className={styles.ghostLink}
              >
                {t('recaps.backToTrip')}
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
