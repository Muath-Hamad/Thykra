import { Link } from '@tanstack/react-router';
import { getAccessToken } from '../api/client';
import { useT } from '../i18n/LocaleProvider';
import { useDocumentTitle } from '../ui/hooks';

/** One plate, one line, one route back. No illustration, no search box. */
export function NotFoundPage() {
  const t = useT();
  const authed = !!getAccessToken();
  useDocumentTitle(`${t('notFound.title')} · Thykra`);

  return (
    <div
      style={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'var(--sp-6)',
      }}
    >
      <div style={{ display: 'flex', gap: 20, alignItems: 'center', maxWidth: 480 }}>
        <div
          aria-hidden="true"
          style={{
            width: 84,
            height: 60,
            flex: 'none',
            background: 'var(--bg-sunken)',
            border: '1px dashed var(--rule-strong)',
            transform: 'rotate(-3deg)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <span className="t-label">404</span>
        </div>
        <div>
          <h1 className="t-display-s">{t('notFound.title')}</h1>
          <p style={{ marginTop: 8 }}>
            <Link to={authed ? '/trips' : '/'}>
              {authed ? t('notFound.cta.auth') : t('notFound.cta.anon')}
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
