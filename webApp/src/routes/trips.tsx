import {
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type RefObject,
} from 'react';
import { useNavigate, useSearch } from '@tanstack/react-router';
import { Plus } from 'lucide-react';
import { createAlbum, getAlbums, type AlbumDto } from '../api/albums';
import { getAlbumMedia, type MediaDto } from '../api/media';
import { chromeStyles } from '../app/AppShell';
import { useAuth } from '../auth/AuthContext';
import { useLocale, type StringKey } from '../i18n/LocaleProvider';
import { timeOfDay, weekdayName } from '../lib/format';
import { countNewSince, getLastVisit } from '../lib/lastVisit';
import { Button } from '../ui/Button';
import { TripCard, TripCardSkeleton, type TripCardActivity } from '../ui/Card';
import { EmptyState } from '../ui/EmptyState';
import { Input, Textarea } from '../ui/Field';
import { useDocumentTitle } from '../ui/hooks';
import { Modal, ModalBody } from '../ui/Modal';
import { Delayed } from '../ui/Skeleton';
import { Segmented } from '../ui/Tabs';
import { useToast } from '../ui/Toast';
import styles from './trips.module.css';

/** Media is prefetched for at most this many albums — the counts line drops
 *  the photo figure rather than guessing when not everything is known. */
const MEDIA_PREFETCH_LIMIT = 8;
const WEEK_MS = 7 * 24 * 60 * 60 * 1000;

type TripFilter = 'recent' | 'mine' | 'shared';

type LoadError = { kind: 'server'; message?: string } | { kind: 'network' } | null;

function firstNameOf(displayName: string | undefined): string {
  return displayName?.trim().split(/\s+/)[0] ?? '';
}

/**
 * "Sara added 12 photos · 2h ago" — top uploader other than the viewer over
 * the last 7 days, derived from media uploadedAt. Null when nothing recent
 * or the uploader's name is not in previewMembers (never show a blank name).
 */
function deriveCardActivity(
  album: AlbumDto,
  media: MediaDto[] | undefined,
  viewerId: string | undefined,
): TripCardActivity | null {
  if (!media || media.length === 0) return null;
  const cutoff = Date.now() - WEEK_MS;
  const byUploader = new Map<string, { count: number; latest: string }>();
  for (const m of media) {
    if (m.uploaderId === viewerId) continue;
    const at = Date.parse(m.uploadedAt);
    if (Number.isNaN(at) || at < cutoff) continue;
    const entry = byUploader.get(m.uploaderId);
    if (!entry) {
      byUploader.set(m.uploaderId, { count: 1, latest: m.uploadedAt });
    } else {
      entry.count += 1;
      if (at > Date.parse(entry.latest)) entry.latest = m.uploadedAt;
    }
  }
  let topId: string | null = null;
  let top: { count: number; latest: string } | null = null;
  for (const [userId, entry] of byUploader) {
    if (!top || entry.count > top.count) {
      topId = userId;
      top = entry;
    }
  }
  if (!top || !topId) return null;
  const finalId = topId;
  const member = album.previewMembers.find((p) => p.userId === finalId);
  if (!member) return null;
  return { actorName: firstNameOf(member.displayName), count: top.count, when: top.latest };
}

export function TripsPage() {
  const { t, lang } = useLocale();
  const { user } = useAuth();
  const navigate = useNavigate();
  const search = useSearch({ strict: false }) as { new?: string };
  const { toast } = useToast();

  useDocumentTitle(`${t('nav.trips')} · ${t('app.name')}`);

  const [albums, setAlbums] = useState<AlbumDto[] | null>(null);
  const [loadError, setLoadError] = useState<LoadError>(null);
  const [mediaByAlbum, setMediaByAlbum] = useState<Record<string, MediaDto[]>>({});
  const [mediaSettled, setMediaSettled] = useState(false);
  const [filter, setFilter] = useState<TripFilter>('recent');
  const [createOpen, setCreateOpen] = useState(false);
  const loadId = useRef(0);

  const load = useCallback(async () => {
    const id = ++loadId.current;
    setAlbums(null);
    setLoadError(null);
    setMediaByAlbum({});
    setMediaSettled(false);
    try {
      const res = await getAlbums();
      if (id !== loadId.current) return;
      if (!res.success || !res.data) {
        setLoadError({ kind: 'server', message: res.error });
        return;
      }
      const list = res.data;
      setAlbums(list);
      // Lazy per-album media — at most the first 8, in parallel. Powers the
      // photo total, "n new" badges, activity lines and the Recent sort.
      void Promise.all(
        list.slice(0, MEDIA_PREFETCH_LIMIT).map(async (album) => {
          try {
            const mediaRes = await getAlbumMedia(album.id);
            return mediaRes.success && mediaRes.data
              ? ([album.id, mediaRes.data] as const)
              : null;
          } catch {
            return null;
          }
        }),
      ).then((entries) => {
        if (id !== loadId.current) return;
        const known: Record<string, MediaDto[]> = {};
        for (const entry of entries) if (entry) known[entry[0]] = entry[1];
        setMediaByAlbum(known);
        setMediaSettled(true);
      });
    } catch {
      if (id === loadId.current) setLoadError({ kind: 'network' });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // AppNav's New trip navigates to /trips?new=1 — open the modal and strip
  // the param so refresh/back does not reopen it.
  useEffect(() => {
    if (search.new === '1') {
      setCreateOpen(true);
      void navigate({ to: '/trips', search: {}, replace: true });
    }
  }, [search.new, navigate]);

  const tod = timeOfDay();
  const firstName = firstNameOf(user?.displayName);
  const isEmptyRun = albums !== null && albums.length === 0;
  const greeting = isEmptyRun
    ? t('trips.greeting.welcome')
    : t(`trips.greeting.${tod}` as StringKey);

  const peopleCount = useMemo(() => {
    if (!albums) return 0;
    const ids = new Set<string>();
    for (const album of albums) for (const p of album.previewMembers) ids.add(p.userId);
    if (user) ids.delete(user.id);
    return ids.size;
  }, [albums, user]);

  const allMediaKnown =
    albums !== null && albums.length > 0 && albums.every((a) => mediaByAlbum[a.id] !== undefined);
  const photoTotal = allMediaKnown
    ? albums.reduce((sum, a) => sum + (mediaByAlbum[a.id]?.length ?? 0), 0)
    : null;

  const mineCount = useMemo(
    () => albums?.filter((a) => a.ownerId === user?.id).length ?? 0,
    [albums, user?.id],
  );
  const sharedCount = (albums?.length ?? 0) - mineCount;

  const visible = useMemo(() => {
    if (!albums) return [];
    if (filter === 'mine') return albums.filter((a) => a.ownerId === user?.id);
    if (filter === 'shared') return albums.filter((a) => a.ownerId !== user?.id);
    // Recent — newest activity: newest media uploadedAt where known, else createdAt.
    const stamp = (a: AlbumDto) => {
      let latest = Date.parse(a.createdAt) || 0;
      const media = mediaByAlbum[a.id];
      if (media) {
        for (const m of media) {
          const at = Date.parse(m.uploadedAt);
          if (at > latest) latest = at;
        }
      }
      return latest;
    };
    return [...albums].sort((x, y) => stamp(y) - stamp(x));
  }, [albums, filter, mediaByAlbum, user?.id]);

  const errorMessage =
    loadError?.kind === 'server'
      ? loadError.message || t('common.errorGeneric')
      : t('common.connectionError');

  return (
    <div className={`page-enter ${styles.page}`}>
      <header className={styles.band}>
        <div>
          <p className="t-label">
            {t(`trips.dayLabel.${tod}` as StringKey, { day: weekdayName(lang) })}
          </p>
          <h1 className={`t-display-m ${styles.headline}`}>
            {firstName ? (
              <>
                {greeting}
                <span className="accent-phrase">{firstName}.</span>
              </>
            ) : (
              greeting.replace(/[,،]\s*$/, '')
            )}
          </h1>
          {albums !== null && albums.length > 0 && mediaSettled && (
            <p className={`t-body-s ${styles.counts}`}>
              {photoTotal !== null
                ? t('trips.counts', {
                    trips: albums.length,
                    photos: photoTotal,
                    people: peopleCount,
                  })
                : t('trips.counts.noPhotos', { trips: albums.length, people: peopleCount })}
            </p>
          )}
        </div>
        {albums !== null && albums.length > 0 && (
          <Segmented<TripFilter>
            options={[
              { value: 'recent', label: t('trips.filter.recent') },
              { value: 'mine', label: t('trips.filter.mine'), disabled: mineCount === 0 },
              { value: 'shared', label: t('trips.filter.shared'), disabled: sharedCount === 0 },
            ]}
            value={filter}
            onChange={setFilter}
            label={t('trips.filter.label')}
          />
        )}
      </header>

      {albums === null && loadError === null && (
        <Delayed>
          <div className={styles.grid}>
            {Array.from({ length: 6 }, (_, i) => (
              <TripCardSkeleton key={i} />
            ))}
          </div>
        </Delayed>
      )}

      {loadError !== null && (
        <div className={styles.stateArea}>
          <EmptyState
            kind="error"
            title={t('trips.error.title')}
            body={errorMessage}
            actions={
              <Button onClick={() => void load()}>{t('common.retry')}</Button>
            }
          />
        </div>
      )}

      {isEmptyRun && (
        <div className={styles.stateArea}>
          <EmptyState
            title={t('trips.empty.title')}
            body={t('trips.empty.body')}
            actions={
              <Button variant="warm" onClick={() => setCreateOpen(true)}>
                {t('trips.empty.cta')}
              </Button>
            }
          />
        </div>
      )}

      {albums !== null && albums.length > 0 && (
        <div className={styles.grid}>
          {visible.map((album) => {
            const media = mediaByAlbum[album.id];
            return (
              <TripCard
                key={album.id}
                album={album}
                newCount={media ? countNewSince(media, getLastVisit(album.id), user?.id) : 0}
                activity={deriveCardActivity(album, media, user?.id)}
                photoCount={media ? media.length : undefined}
              />
            );
          })}
        </div>
      )}

      {/* Mobile FAB — replaces the header New trip button below 768px
          (chrome ships the style; display:none on desktop). */}
      <button
        type="button"
        className={chromeStyles.fab}
        aria-label={t('nav.newTrip')}
        onClick={() => setCreateOpen(true)}
      >
        <Plus size={24} aria-hidden="true" />
      </button>

      <CreateTripModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={(album) => {
          toast({ kind: 'success', title: t('trips.create.toast') });
          void navigate({ to: '/trips/$albumId', params: { albumId: album.id } });
        }}
      />
    </div>
  );
}

/**
 * Create-trip modal. No optimistic insert — the server assigns the id; on
 * success the app navigates straight into the new trip (its page moves
 * focus to the h1). Request errors land in the modal error strip with the
 * inputs untouched.
 */
function CreateTripModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (album: AlbumDto) => void;
}) {
  const { t } = useLocale();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [nameError, setNameError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const nameRef = useRef<HTMLInputElement | null>(null);
  const formId = useId();

  useEffect(() => {
    if (open) {
      setName('');
      setDescription('');
      setNameError(null);
      setSubmitError(null);
      setSubmitting(false);
    }
  }, [open]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (submitting) return;
    const trimmed = name.trim();
    if (!trimmed) {
      setNameError(t('trips.create.nameError'));
      nameRef.current?.focus();
      return;
    }
    setNameError(null);
    setSubmitError(null);
    setSubmitting(true);
    try {
      const res = await createAlbum(trimmed, description.trim() || undefined);
      if (res.success && res.data) {
        onClose();
        onCreated(res.data);
        return;
      }
      setSubmitError(res.error || t('common.errorGeneric'));
    } catch {
      setSubmitError(t('common.connectionError'));
    }
    setSubmitting(false);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      eyebrow={t('trips.create.label')}
      title={t('trips.create.title')}
      busy={submitting}
      error={submitError}
      initialFocusRef={nameRef as RefObject<HTMLElement | null>}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={submitting}>
            {t('common.cancel')}
          </Button>
          <Button
            type="submit"
            form={formId}
            loading={submitting}
            loadingLabel={t('trips.create.submitting')}
          >
            {t('trips.create.submit')}
          </Button>
        </>
      }
    >
      <ModalBody>
        {/* noValidate: required-ness is announced via the inline error, not
            the browser bubble. Enter in the name field submits natively. */}
        <form id={formId} className={styles.form} onSubmit={(e) => void onSubmit(e)} noValidate>
          <Input
            ref={nameRef}
            label={t('trips.create.name')}
            placeholder={t('trips.create.namePlaceholder')}
            maxLength={80}
            required
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              if (nameError) setNameError(null);
            }}
            error={nameError}
            autoComplete="off"
          />
          <Textarea
            label={t('trips.create.description')}
            optionalTag
            placeholder={t('trips.create.descriptionPlaceholder')}
            maxLength={300}
            showCounter
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </form>
      </ModalBody>
    </Modal>
  );
}
