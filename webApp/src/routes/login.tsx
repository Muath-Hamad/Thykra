import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { useNavigate, useSearch } from '@tanstack/react-router';
import { GoogleLogin, type CredentialResponse } from '@react-oauth/google';
import { getInvitePreview } from '../api/invites';
import type { AlbumDto } from '../api/albums';
import { useAuth } from '../auth/AuthContext';
import { useLocale } from '../i18n/LocaleProvider';
import { PENDING_INVITE_KEY } from './invite';
import styles from './login.module.css';

const HERO_PHOTO =
  'https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=1920&q=85';

function readStash(): string | null {
  try {
    return localStorage.getItem(PENDING_INVITE_KEY);
  } catch {
    return null;
  }
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

export function LoginPage() {
  const search = useSearch({ strict: false }) as { next?: string; error?: string };
  const navigate = useNavigate();
  const auth = useAuth();
  const { t } = useLocale();

  const [stash, setStash] = useState<string | null>(null);
  const [inviteAlbum, setInviteAlbum] = useState<AlbumDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(
    search.error ? t('login.rejected', { reason: search.error }) : null,
  );

  // ── Why am I here? The stashed invite token answers it. ──
  useEffect(() => {
    const token = readStash();
    if (!token) return;
    setStash(token);
    let cancelled = false;
    getInvitePreview(token)
      .then((res) => {
        if (cancelled) return;
        const data = res.success ? res.data : undefined;
        if (
          data?.album &&
          (data.status === 'VALID' || data.status === 'ALREADY_MEMBER')
        ) {
          setInviteAlbum(data.album);
        }
      })
      .catch(() => {
        // The panel simply falls back to the title-less copy.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const goAfterSignIn = useCallback(() => {
    // 1 — a stashed invite beats everything; the invite page consumes it.
    if (stash) {
      void navigate({ to: '/invite/$token', params: { token: stash }, replace: true });
      return;
    }
    // 2 — the return path we were sent here with.
    const next = search.next;
    if (typeof next === 'string' && next.startsWith('/')) {
      try {
        void navigate({ to: next as never, replace: true }).catch(() => {
          window.location.assign(next);
        });
      } catch {
        window.location.assign(next);
      }
      return;
    }
    // 3 — home.
    void navigate({ to: '/trips', search: {}, replace: true });
  }, [navigate, search.next, stash]);

  const onCredential = useCallback(
    async (credentialResponse: CredentialResponse) => {
      setError(null);
      setBusy(true);
      try {
        const response = await fetch('/api/auth/oauth', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            provider: 'GOOGLE',
            idToken: credentialResponse.credential,
          }),
        });
        const data = await response.json();
        if (data?.success && data.data) {
          auth.login(data.data.accessToken, data.data.refreshToken, data.data.user);
          goAfterSignIn();
          return;
        }
        setBusy(false);
        setError(t('login.rejected', { reason: data?.error ?? '' }));
      } catch {
        setBusy(false);
        setError(t('common.connectionError'));
      }
    },
    [auth, goAfterSignIn, t],
  );

  const hasInvite = stash !== null;
  const headline = hasInvite ? t('login.oneStepLeft') : t('login.title');

  return (
    <div className={`page-enter ${styles.page}`}>
      <div className={styles.photoSide}>
        <img
          className={styles.photo}
          src={inviteAlbum?.coverUrl ?? HERO_PHOTO}
          alt=""
          aria-hidden="true"
        />
        <div className={styles.photoOverlay}>
          {inviteAlbum ? (
            <>
              <div className={`t-label ${styles.overlayLabel}`}>
                {t('login.invitePanel.imageLabel')}
              </div>
              <div className={`t-display-s ${styles.overlayTitle}`}>{inviteAlbum.title}</div>
            </>
          ) : (
            <div className={`t-display-s ${styles.quote}`}>
              {t('login.quoteBefore')}
              <br />
              <em className={styles.quoteAccent}>{t('login.quoteAccent')}</em>
            </div>
          )}
        </div>
      </div>

      <div className={styles.formSide}>
        <div className={styles.card}>
          <span className={styles.wordmark}>Thykra</span>

          {hasInvite && (
            <div className={styles.invitePanel} role="status">
              <span className="t-body-s">
                {inviteAlbum
                  ? emphasise(
                      t('login.invitePanel.known', { title: inviteAlbum.title }),
                      inviteAlbum.title,
                    )
                  : t('login.invitePanel.unknown')}
              </span>
            </div>
          )}

          <h1 className={`t-display-s ${styles.title}`}>{headline}</h1>
          <p className={`t-body ${styles.sub}`}>{t('login.sub')}</p>

          {error && (
            <div className={styles.errorStrip} role="alert">
              <span className="t-body-s">{error}</span>
            </div>
          )}

          <div className={styles.buttonArea}>
            {busy ? (
              <div className={styles.authenticating}>
                <span className={styles.spinner} aria-hidden="true" />
                <span className="t-body">{t('login.authenticating')}</span>
              </div>
            ) : (
              /* The real Google button — never a restyled imitation. */
              <GoogleLogin
                onSuccess={(credentialResponse) => void onCredential(credentialResponse)}
                onError={() => setError(t('login.cancelled'))}
                width="320"
              />
            )}
          </div>

          <p className={`t-meta ${styles.terms}`}>{t('login.terms')}</p>
        </div>
      </div>
    </div>
  );
}
