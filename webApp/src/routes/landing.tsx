import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useSearch } from '@tanstack/react-router';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../auth/AuthContext';
import { useLocale } from '../i18n/LocaleProvider';
import { usePaperOnly } from '../theme/ThemeProvider';
import { AvatarStack } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import { Plate } from '../ui/Plate';
import { useDocumentTitle, useMediaQuery } from '../ui/hooks';
import { PENDING_INVITE_KEY } from './invite';
import styles from './landing.module.css';

const HERO_PHOTO =
  'https://images.unsplash.com/photo-1539635278303-d4002c07eae3?w=1200&q=80';

/** The three people in the floating proof card — decorative, not real users. */
const DEMO_PEOPLE = [
  { userId: 'demo1', displayName: 'Muath' },
  { userId: 'demo2', displayName: 'Sara' },
  { userId: 'demo3', displayName: 'Omar' },
];

/**
 * / — the front door. One job: make a signed-out visitor understand where the
 * group's photos go, then hand them to Google. No API calls, no feature grid;
 * this page must paint on the first byte.
 */
export function LandingPage() {
  // Marketing ships light-only — Darkroom is for the app, not the shopfront.
  usePaperOnly();

  const { t, lang, setLang } = useLocale();
  const auth = useAuth();
  const navigate = useNavigate();
  const search = useSearch({ strict: false }) as { error?: string };
  // Below 1024 the hero stacks and the plate turns landscape.
  const stacked = useMediaQuery('(max-width: 1023px)');
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  useDocumentTitle('Thykra — Travel Together. Remember Forever.');

  // The landing signs people in directly — the real Google element, never a
  // restyled fake. Same exchange + resume order as /login.
  const handleGoogleSuccess = async (credentialResponse: { credential?: string }) => {
    const idToken = credentialResponse.credential;
    if (!idToken) return;
    try {
      setAuthError(null);
      setAuthBusy(true);
      const response = await fetch('/api/auth/oauth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider: 'GOOGLE', idToken }),
      });
      const data = await response.json();
      if (data.success && data.data) {
        auth.login(data.data.accessToken, data.data.refreshToken, data.data.user);
        let stash: string | null = null;
        try {
          stash = localStorage.getItem(PENDING_INVITE_KEY);
        } catch {
          /* private mode */
        }
        if (stash) {
          void navigate({ to: '/invite/$token', params: { token: stash }, replace: true });
        } else {
          void navigate({ to: '/trips', search: {}, replace: true });
        }
      } else {
        setAuthError(t('landing.oauthError'));
        setAuthBusy(false);
      }
    } catch {
      setAuthError(t('landing.oauthError'));
      setAuthBusy(false);
    }
  };

  const googleButton = (
    <GoogleLogin
      onSuccess={handleGoogleSuccess}
      onError={() => setAuthError(t('landing.oauthError'))}
      text="continue_with"
      size="large"
      locale={lang}
    />
  );

  return (
    <div className={`page-enter ${styles.root}`}>
      <a href="#content" className="skip-link">
        {t('app.skipToContent')}
      </a>

      {/* ── Top bar ── */}
      <header className={styles.topBar}>
        <Link to="/" className={styles.wordmark}>
          {t('app.name')}
        </Link>
        <nav className={styles.topNav}>
          <a href="#how" className={styles.navLink}>
            {t('landing.howItWorks')}
          </a>
          <button
            type="button"
            className={styles.langToggle}
            onClick={() => setLang(lang === 'en' ? 'ar' : 'en')}
          >
            {t('landing.langToggle')}
          </button>
          <Button as="a" href="/login" size="sm">
            {t('nav.signIn')}
          </Button>
        </nav>
      </header>

      {/* OAuth bounce-back — a strip under the bar, never a modal. */}
      {(search.error || authError) && (
        <div className={styles.errorStrip} role="alert">
          {authError ?? t('landing.oauthError')}
        </div>
      )}

      <main id="content">
        {/* ── Hero ── */}
        <section className={styles.hero}>
          <div className={styles.heroText}>
            <span className={`t-label ${styles.tagline}`}>{t('landing.tagline')}</span>
            <h1 className={`t-display-xl ${styles.headline}`}>
              {t('landing.heroBefore')}
              <em className="accent-phrase">{t('landing.heroAccent')}</em>
            </h1>
            <p className={`t-body-l ${styles.sub}`}>{t('landing.sub')}</p>
            <div className={styles.ctaRow}>
              {authBusy ? (
                <span className={styles.authBusyRow} role="status">
                  <span className={styles.authSpinner} aria-hidden="true" />
                  {t('login.authenticating')}
                </span>
              ) : (
                googleButton
              )}
              <span className={`t-meta ${styles.googleNote}`}>{t('landing.googleNote')}</span>
            </div>
          </div>

          <div className={styles.heroMedia}>
            <Plate
              src={HERO_PHOTO}
              alt=""
              aspectRatio={stacked ? '16 / 9' : '4 / 5'}
              eager
            />
            {/* Proof that the product has people in it. */}
            <Card className={styles.floatCard}>
              <AvatarStack
                people={DEMO_PEOPLE}
                totalCount={DEMO_PEOPLE.length}
                size="sm"
                ringColor="var(--bg-raised)"
                asDecoration
              />
              <span className={styles.proofLine}>
                <HighlightName
                  text={t('landing.activityProof', { name: 'Sara', n: 12 })}
                  name="Sara"
                />
              </span>
            </Card>
          </div>
        </section>

        {/* ── Three steps, separated by rules, not cards ── */}
        <section id="how" className={styles.steps}>
          {(['1', '2', '3'] as const).map((n) => (
            <div key={n} className={styles.step}>
              <span className={styles.stepNumeral} aria-hidden="true">
                0{n}
              </span>
              <h2 className={`t-title ${styles.stepTitle}`}>
                {t(`landing.step${n}.title` as 'landing.step1.title')}
              </h2>
              <p className={styles.stepBody}>
                {t(`landing.step${n}.body` as 'landing.step1.body')}
              </p>
            </div>
          ))}
        </section>
      </main>

      {/* ── Ink footer band — the second sign-in ── */}
      <footer className={styles.footerBand}>
        <div className={styles.footerInner}>
          <p className={`t-display-s ${styles.footerLine}`}>{t('landing.footerLine')}</p>
          <div className={styles.footerCta}>{!authBusy && googleButton}</div>
        </div>
      </footer>
    </div>
  );
}

/** Renders an interpolated sentence with the person's name in clay. */
function HighlightName({ text, name }: { text: string; name: string }): ReactNode {
  const at = text.indexOf(name);
  if (at < 0) return text;
  return (
    <>
      {text.slice(0, at)}
      <span className={styles.proofName}>{name}</span>
      {text.slice(at + name.length)}
    </>
  );
}
