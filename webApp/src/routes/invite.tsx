import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from '@tanstack/react-router';
import { joinByInvite, type AlbumDto } from '../api/albums';
import { getInvitePreview, type InvitePreviewDto } from '../api/invites';
import { useAuth } from '../auth/AuthContext';
import { useLocale } from '../i18n/LocaleProvider';
import { formatDateMedium } from '../lib/format';
import { Avatar, AvatarStack } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { Plate } from '../ui/Plate';
import { Delayed, Skeleton } from '../ui/Skeleton';
import { Stamp } from '../ui/Stamp';
import { useToast } from '../ui/Toast';
import { useDocumentTitle, useMediaQuery } from '../ui/hooks';
import styles from './invite.module.css';

/** Where a signed-out visitor's invite token waits while they sign in. */
export const PENDING_INVITE_KEY = 'thykra_pending_invite_token';

type LoadPhase = 'loading' | 'ready' | 'netError';
type JoinPhase = 'idle' | 'joining' | 'failed' | 'success';

const DAY_MS = 24 * 60 * 60 * 1000;

/** A stash older than this is an abandoned invite, not an intent. */
const STASH_MAX_AGE_MS = 30 * 60 * 1000;

function stashToken(token: string) {
  try {
    localStorage.setItem(PENDING_INVITE_KEY, JSON.stringify({ token, at: Date.now() }));
  } catch {
    /* private mode — the flow still works, it just forgets */
  }
}

export function forgetToken() {
  try {
    localStorage.removeItem(PENDING_INVITE_KEY);
  } catch {
    /* private mode */
  }
}

/**
 * The stashed token, or null. Entries older than 30 minutes are dropped so an
 * abandoned invite never hijacks a later sign-in. A value written before the
 * stash carried a timestamp is a bare token — honoured once, then consumed.
 */
export function readPendingInvite(): string | null {
  let raw: string | null = null;
  try {
    raw = localStorage.getItem(PENDING_INVITE_KEY);
  } catch {
    return null;
  }
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as { token?: unknown; at?: unknown };
    if (parsed && typeof parsed.token === 'string') {
      if (typeof parsed.at === 'number' && Date.now() - parsed.at > STASH_MAX_AGE_MS) {
        forgetToken();
        return null;
      }
      return parsed.token;
    }
  } catch {
    /* legacy shape — the raw string is the token */
  }
  return raw;
}

/** Emphasise one run of characters inside an already-interpolated sentence. */
function emphasise(text: string, needle: string): ReactNode {
  if (!needle) return text;
  const at = text.indexOf(needle);
  if (at < 0) return text;
  return (
    <>
      {text.slice(0, at)}
      <strong className={styles.strong}>{needle}</strong>
      {text.slice(at + needle.length)}
    </>
  );
}

export function InvitePage() {
  const { token } = useParams({ strict: false }) as { token: string };
  const navigate = useNavigate();
  const { t, lang } = useLocale();
  const { toast } = useToast();
  const auth = useAuth();
  const isMobile = useMediaQuery('(max-width: 767px)');

  const [phase, setPhase] = useState<LoadPhase>('loading');
  const [preview, setPreview] = useState<InvitePreviewDto | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [joinPhase, setJoinPhase] = useState<JoinPhase>('idle');
  const [joinedAlbum, setJoinedAlbum] = useState<AlbumDto | null>(null);

  // ── Preview ──
  useEffect(() => {
    let cancelled = false;
    setPhase('loading');
    getInvitePreview(token)
      .then((res) => {
        if (cancelled) return;
        if (res.success && res.data) {
          setPreview(res.data);
        } else {
          // A 404 or a rejected envelope is a dead link, not a dead network.
          setPreview({ status: 'REVOKED', previewMedia: [] });
        }
        setPhase('ready');
      })
      .catch(() => {
        // The request itself failed — never confuse that with a revoked invite.
        if (!cancelled) setPhase('netError');
      });
    return () => {
      cancelled = true;
    };
  }, [token, attempt]);

  const album = preview?.album ?? null;
  const invitedBy = preview?.invitedBy ?? null;
  const mediaCount = preview?.mediaCount ?? null;
  const expiresAt = preview?.expiresAt ?? null;
  const previewMedia = preview?.previewMedia ?? [];

  useDocumentTitle(album ? t('invite.pageTitle', { title: album.title }) : null);

  // ── Success hold, then into the trip ──
  useEffect(() => {
    if (joinPhase !== 'success' || !joinedAlbum) return;
    const handle = window.setTimeout(() => {
      void navigate({
        to: '/trips/$albumId',
        params: { albumId: joinedAlbum.id },
        search: { joined: '1' },
        replace: true,
      });
    }, 700);
    return () => window.clearTimeout(handle);
  }, [joinPhase, joinedAlbum, navigate]);

  const goToLogin = useCallback(() => {
    stashToken(token);
    void navigate({ to: '/login', search: {} });
  }, [navigate, token]);

  const onJoin = useCallback(async () => {
    if (!auth.isAuthenticated) {
      goToLogin();
      return;
    }
    setJoinPhase('joining');
    try {
      const res = await joinByInvite(token);
      if (res.success && res.data) {
        forgetToken();
        setJoinedAlbum(res.data);
        setJoinPhase('success');
      } else {
        setJoinPhase('failed');
      }
    } catch {
      setJoinPhase('failed');
    }
  }, [auth.isAuthenticated, goToLogin, token]);

  const useAnotherAccount = useCallback(() => {
    stashToken(token);
    // logout() needs the access token to reach POST /api/auth/logout — and it
    // clears the tokens itself once the call is away.
    auth.logout();
    void navigate({ to: '/login', search: {} });
  }, [auth, navigate, token]);

  const copyExpiredMessage = useCallback(() => {
    const body = t('invite.expired.messageBody', {
      name: invitedBy?.displayName ?? '',
      title: album?.title ?? '',
    });
    const done = () => toast({ kind: 'success', title: t('invite.expired.messageCopied') });
    try {
      void navigator.clipboard.writeText(body).then(done, () => {});
    } catch {
      /* clipboard unavailable — nothing to announce */
    }
  }, [album, invitedBy, t, toast]);

  const expiresSoon = useMemo(() => {
    if (!expiresAt) return false;
    const ms = new Date(expiresAt).getTime() - Date.now();
    return Number.isFinite(ms) && ms < DAY_MS;
  }, [expiresAt]);

  // ── Pieces shared by the VALID layouts ──

  const stampNode =
    invitedBy != null ? (
      <Stamp
        tone="warm"
        eyebrow={t('invite.stamp')}
        name={invitedBy.displayName}
        avatar={
          <Avatar
            size="sm"
            decorative
            userId="inviter"
            displayName={invitedBy.displayName}
            avatarUrl={invitedBy.avatarUrl}
          />
        }
      />
    ) : (
      <Stamp tone="warm" name={t('invite.stamp.private')} />
    );

  const whatLines = [
    mediaCount != null ? t('invite.what1.photosOnly', { n: mediaCount }) : t('invite.what1.generic'),
    t('invite.what2'),
    t('invite.what3'),
  ];

  const ctaLabel = joinPhase === 'failed' ? t('invite.failed.cta') : t('invite.cta');

  const ctaButton = (
    <Button
      variant="warm"
      size="lg"
      loading={joinPhase === 'joining'}
      loadingLabel={t('invite.joining')}
      onClick={() => void onJoin()}
    >
      {ctaLabel}
    </Button>
  );

  const signedInPanel =
    auth.isAuthenticated && auth.user ? (
      <div className={styles.signedInPanel}>
        <Avatar
          size="sm"
          decorative
          userId={auth.user.id}
          displayName={auth.user.displayName}
          avatarUrl={auth.user.avatarUrl}
        />
        <span className="t-body-s">{t('invite.signedInAs', { name: auth.user.displayName })}</span>
        <button type="button" className={styles.inlineLink} onClick={useAnotherAccount}>
          {t('invite.useAnother')}
        </button>
      </div>
    ) : null;

  const contentBody = album ? (
    <>
      <div className={`t-label ${styles.microLabel}`}>{t('invite.microLabel')}</div>
      <h1 className={`t-display-l ${styles.title}`}>{album.title}</h1>
      {album.description && <p className={`t-body-l ${styles.description}`}>{album.description}</p>}

      <div className={styles.peopleRow}>
        <AvatarStack people={album.previewMembers} totalCount={album.memberCount} asDecoration />
        <span className="t-body-s">
          {emphasise((album.memberCount === 1 ? t('invite.people.one') : t('invite.people', { n: album.memberCount })), String(album.memberCount))}
        </span>
      </div>

      {signedInPanel}

      {joinPhase === 'failed' && (
        <div className={styles.failStrip} role="alert">
          <strong className={styles.strong}>{t('invite.failed.title')}</strong>
          <span className="t-body-s">{t('invite.failed.body')}</span>
        </div>
      )}
    </>
  ) : null;

  const whatBlock = (
    <>
      <ul className={styles.whatList}>
        {whatLines.map((line) => (
          <li key={line} className="t-body-s">
            {line}
          </li>
        ))}
      </ul>
      {expiresAt && (
        <p className={`t-meta ${styles.expiry}`}>
          {expiresSoon
            ? t('invite.expiry.today')
            : t('invite.expiry', { date: formatDateMedium(expiresAt, lang) })}
        </p>
      )}
    </>
  );

  const coverPlate = (aspect: string) =>
    album?.coverUrl ? (
      <Plate src={album.coverUrl} alt="" aspectRatio={aspect} eager />
    ) : (
      <div className={styles.blankPlate} style={{ aspectRatio: aspect }} aria-hidden="true">
        <span className="t-label">{t('trips.card.noPhotos')}</span>
      </div>
    );

  const coverColumn = (
    <aside className={styles.coverCol}>
      <div className={styles.coverFrame}>{coverPlate('4 / 3')}</div>
      {previewMedia.length > 0 && (
        <div className={styles.teaser} aria-hidden="true">
          {previewMedia.slice(0, 3).map((m, i) => (
            <img key={i} className={styles.teaserThumb} src={m.thumbnailUrl} alt="" />
          ))}
        </div>
      )}
      <div className={styles.coverFooter}>
        <span className="t-label">{t('invite.inside')}</span>
        {mediaCount != null && mediaCount - previewMedia.length > 0 && (
          <span className="t-meta">
            {t('invite.insideMore', { n: mediaCount - previewMedia.length })}
          </span>
        )}
      </div>
    </aside>
  );

  // ── One renderer per state; the shell never re-lays-out around them ──

  function renderLoading() {
    return (
      <div className={styles.split}>
        <div className={styles.contentCol}>
          <Delayed>
            <Skeleton width={150} height={26} radius="var(--r-xs)" />
            <div className={styles.skelTitle}>
              <Skeleton width="74%" height={38} />
            </div>
            <div className={styles.skelLines}>
              <Skeleton width="92%" height={13} />
              <Skeleton width="68%" height={13} />
            </div>
            <div className={styles.skelAvatars}>
              <Skeleton width={26} height={26} radius="50%" />
              <Skeleton width={26} height={26} radius="50%" />
              <Skeleton width={26} height={26} radius="50%" />
              <Skeleton width={26} height={26} radius="50%" />
            </div>
            <div className={styles.skelCta}>
              <Skeleton width={150} height={44} radius="var(--r-sm)" />
            </div>
          </Delayed>
        </div>
        <aside className={styles.coverCol}>
          <Delayed>
            <div className={styles.coverFrame}>
              <Skeleton aspectRatio="4 / 3" radius={0} />
            </div>
          </Delayed>
        </aside>
      </div>
    );
  }

  function renderNetError() {
    return (
      <div className={styles.deadEnd}>
        <EmptyState
          kind="error"
          title={t('common.connectionError')}
          actions={
            <Button variant="secondary" onClick={() => setAttempt((n) => n + 1)}>
              {t('common.retry')}
            </Button>
          }
        />
      </div>
    );
  }

  function renderSuccess() {
    return (
      <div className={styles.celebration}>
        <div className="t-display-m">{t('invite.success.title')}</div>
        <p className={`t-body-l ${styles.celebrationBody}`}>
          {t('invite.success.body', { title: joinedAlbum?.title ?? album?.title ?? '' })}
        </p>
        <div className={styles.progressTrack} aria-hidden="true">
          <div className={styles.progressBar} />
        </div>
      </div>
    );
  }

  function renderValid() {
    if (isMobile) {
      return (
        <>
          <div className={styles.hero}>
            {coverPlate('4 / 5')}
            <Link to="/" className={styles.heroChip}>
              Thykra
            </Link>
            <div className={styles.heroStamp}>{stampNode}</div>
          </div>
          <div className={styles.contentCol}>
            {contentBody}
            {joinPhase === 'joining' && (
              <p className={`t-body-s ${styles.ctaSub}`}>{t('invite.joiningBody')}</p>
            )}
            {whatBlock}
          </div>
          {/* After the content in the DOM, sticky not fixed — tab order intact. */}
          <div className={styles.stickyBar}>
            {ctaButton}
            {!auth.isAuthenticated && (
              <span className={`t-meta ${styles.stickySub}`}>{t('invite.cta.sub')}</span>
            )}
          </div>
        </>
      );
    }
    return (
      <div className={styles.split}>
        <div className={styles.contentCol}>
          {stampNode}
          {contentBody}
          <div className={styles.ctaRow}>
            {ctaButton}
            {joinPhase === 'joining' ? (
              <span className={`t-body-s ${styles.ctaSub}`}>{t('invite.joiningBody')}</span>
            ) : !auth.isAuthenticated ? (
              <span className={`t-body-s ${styles.ctaSub}`}>{t('invite.cta.sub')}</span>
            ) : null}
          </div>
          {whatBlock}
        </div>
        {coverColumn}
      </div>
    );
  }

  function renderExpired() {
    return (
      <div className={styles.deadEnd}>
        <Stamp tone="warn" name={t('invite.expired.stamp')} />
        <h1 className={`t-display-m ${styles.deadTitle}`}>{t('invite.expired.title')}</h1>
        <p className={`t-body-l ${styles.deadBody}`}>
          {album
            ? t('invite.expired.body', {
                title: album.title,
                date: expiresAt ? formatDateMedium(expiresAt, lang) : '',
                name: invitedBy?.displayName ?? '',
              })
            : t('invite.expired.bodyUnknown', {
                date: expiresAt ? formatDateMedium(expiresAt, lang) : '',
              })}
        </p>
        <div className={styles.deadActions}>
          <Button onClick={copyExpiredMessage}>
            {t('invite.expired.copyMessage', { name: invitedBy?.displayName ?? '' })}
          </Button>
          <Button as="a" variant="secondary" href="/">
            {t('common.whatIsThykra')}
          </Button>
        </div>
      </div>
    );
  }

  function renderAlreadyMember() {
    const when = preview?.joinedAt ?? album?.createdAt ?? null;
    return (
      <div className={styles.deadEnd}>
        <div className={styles.checkTile} aria-hidden="true">
          ✓
        </div>
        <h1 className={`t-display-m ${styles.deadTitle}`}>{t('invite.already.title')}</h1>
        <p className={`t-body-l ${styles.deadBody}`}>
          {t('invite.already.body', {
            title: album?.title ?? '',
            date: when ? formatDateMedium(when, lang) : '',
          })}
        </p>
        {album && (
          <div className={styles.deadActions}>
            <Button
              variant="warm"
              size="lg"
              onClick={() => {
                forgetToken();
                void navigate({
                  to: '/trips/$albumId',
                  params: { albumId: album.id },
                  search: {},
                });
              }}
            >
              {t('invite.already.cta')}
            </Button>
          </div>
        )}
      </div>
    );
  }

  function renderBlocked() {
    // No trip name, no cover, no action of any kind.
    return (
      <div className={styles.deadEnd}>
        <h1 className={`t-display-m ${styles.deadTitle}`}>{t('invite.blocked.title')}</h1>
        <p className={`t-body-l ${styles.deadBody}`}>{t('invite.blocked.body')}</p>
      </div>
    );
  }

  function renderRevoked() {
    return (
      <div className={styles.deadEnd}>
        <div className={styles.blankDevice} aria-hidden="true">
          <span className="t-label">{t('invite.revoked.plateLabel')}</span>
        </div>
        <h1 className={`t-display-m ${styles.deadTitle}`}>{t('invite.revoked.title')}</h1>
        <p className={`t-body-l ${styles.deadBody}`}>{t('invite.revoked.body')}</p>
        <div className={styles.deadActions}>
          <Button as="a" variant="secondary" href="/">
            {t('common.whatIsThykra')}
          </Button>
        </div>
      </div>
    );
  }

  function renderBody() {
    if (phase === 'netError') return renderNetError();
    if (phase === 'loading' || !preview) return renderLoading();
    if (joinPhase === 'success') return renderSuccess();
    switch (preview.status) {
      case 'VALID':
        return album ? renderValid() : renderRevoked();
      case 'EXPIRED':
        return renderExpired();
      case 'ALREADY_MEMBER':
        return renderAlreadyMember();
      case 'BLOCKED':
        return renderBlocked();
      default:
        return renderRevoked();
    }
  }

  // The mobile cover carries its own wordmark chip, so the bar would double up.
  const showTopBar = !(
    isMobile &&
    phase === 'ready' &&
    preview?.status === 'VALID' &&
    album != null &&
    joinPhase !== 'success'
  );

  return (
    <div className={`page-enter ${styles.page}`}>
      {showTopBar && (
        <header className={styles.topbar}>
          <Link to="/" className={styles.wordmark}>
            Thykra
          </Link>
          {!auth.isAuthenticated && (
            <span className={styles.topbarTrail}>
              <span className="t-body-s">{t('invite.haveAccount')}</span>
              <Link to="/login" search={{}} className={styles.signInLink}>
                {t('login.title')}
              </Link>
            </span>
          )}
        </header>
      )}
      {/* Every state swap announces through this one region. */}
      <main className={styles.main} aria-live="polite">
        {renderBody()}
      </main>
    </div>
  );
}
