import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { Link, Outlet, useNavigate, useRouterState } from '@tanstack/react-router';
import { Images, Newspaper, Sparkles, CircleUserRound } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useLocale } from '../i18n/LocaleProvider';
import { useTheme } from '../theme/ThemeProvider';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Menu } from '../ui/Menu';
import { UploadDock } from '../upload/UploadDock';
import styles from './AppChrome.module.css';

/**
 * Screens with their own mobile context bar (trip view, settings…) register
 * it here; below 768px it replaces the app nav.
 */
interface ChromeContextType {
  setMobileBar: (node: ReactNode | null) => void;
}
const ChromeContext = createContext<ChromeContextType | null>(null);

export function useMobileContextBar(node: ReactNode | null) {
  const ctx = useContext(ChromeContext);
  useEffect(() => {
    if (!ctx) return;
    ctx.setMobileBar(node);
    return () => ctx.setMobileBar(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ctx, node]);
}

export function AppShell() {
  const [mobileBar, setMobileBar] = useState<ReactNode | null>(null);
  const chromeValue = useMemo(() => ({ setMobileBar }), []);
  const { t } = useLocale();

  return (
    <ChromeContext.Provider value={chromeValue}>
      <a href="#content" className="skip-link">
        {t('app.skipToContent')}
      </a>
      <AppNav mobileHidden={mobileBar !== null} />
      {mobileBar}
      <main id="content" className={styles.main}>
        <Outlet />
      </main>
      <BottomTabs />
      <UploadDock />
    </ChromeContext.Provider>
  );
}

function AppNav({ mobileHidden }: { mobileHidden: boolean }) {
  const { t } = useLocale();
  const { user, logout } = useAuth();
  const { choice, setChoice } = useTheme();
  const navigate = useNavigate();
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  const themeLabel =
    choice === 'system'
      ? t('me.appearance.system')
      : choice === 'light'
        ? t('me.appearance.paper')
        : t('me.appearance.darkroom');

  const isActive = (prefix: string) =>
    pathname === prefix || pathname.startsWith(`${prefix}/`);

  return (
    <header className={[styles.nav, mobileHidden ? styles.hideOnMobile : ''].filter(Boolean).join(' ')}>
      <Link to="/trips" className={styles.wordmark}>
        {t('app.name')}
      </Link>
      <nav className={styles.links} aria-label={t('nav.menu')}>
        <Link
          to="/trips"
          className={styles.link}
          aria-current={isActive('/trips') ? 'page' : undefined}
        >
          {t('nav.trips')}
        </Link>
        <Link
          to="/recaps"
          className={styles.link}
          aria-current={isActive('/recaps') ? 'page' : undefined}
        >
          {t('nav.recaps')}
        </Link>
        <Link
          to="/activity"
          className={styles.link}
          aria-current={isActive('/activity') ? 'page' : undefined}
        >
          {t('nav.activity')}
        </Link>
      </nav>
      <div className={styles.trailing}>
        <span className={styles.newTripDesktop}>
          <Button size="sm" onClick={() => navigate({ to: '/trips', search: { new: '1' } })}>
            {t('nav.newTrip')}
          </Button>
        </span>
        {user && (
          <Menu
            label={t('nav.accountMenu')}
            trigger={(props) => (
              <button type="button" className={styles.avatarButton} aria-label={t('nav.accountMenu')} {...props}>
                <Avatar
                  userId={user.id}
                  displayName={user.displayName}
                  avatarUrl={user.avatarUrl}
                  size="md"
                  decorative
                />
              </button>
            )}
            items={[
              { label: t('nav.you'), onSelect: () => navigate({ to: '/me' }) },
              {
                label: `${t('me.appearance')} · ${themeLabel}`,
                onSelect: () =>
                  setChoice(choice === 'system' ? 'light' : choice === 'light' ? 'dark' : 'system'),
              },
              'separator',
              {
                label: t('nav.signOut'),
                onSelect: () => {
                  logout();
                  navigate({ to: '/' });
                },
              },
            ]}
          />
        )}
      </div>
    </header>
  );
}

function BottomTabs() {
  const { t } = useLocale();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const isActive = (prefix: string) => pathname === prefix || pathname.startsWith(`${prefix}/`);

  const tabs = [
    { to: '/trips', label: t('nav.trips'), icon: Images },
    { to: '/recaps', label: t('nav.recaps'), icon: Newspaper },
    { to: '/activity', label: t('nav.activity'), icon: Sparkles },
    { to: '/me', label: t('nav.you'), icon: CircleUserRound },
  ] as const;

  return (
    <nav className={styles.tabbar} aria-label={t('nav.menu')}>
      {tabs.map((tab) => (
        <Link
          key={tab.to}
          to={tab.to}
          className={styles.tab}
          aria-current={isActive(tab.to) ? 'page' : undefined}
        >
          <tab.icon size={18} aria-hidden="true" />
          <span className={styles.tabLabel}>{tab.label}</span>
        </Link>
      ))}
    </nav>
  );
}

/** Mobile 52px context bar — back chevron, title, one overflow action. */
export function MobileContextBar({
  title,
  onBack,
  end,
}: {
  title: string;
  onBack: () => void;
  end?: ReactNode;
}) {
  const { t } = useLocale();
  return (
    <div className={styles.contextBar}>
      <button type="button" className={styles.contextButton} onClick={onBack} aria-label={t('nav.back')}>
        ‹
      </button>
      <span className={styles.contextTitle}>{title}</span>
      {end}
    </div>
  );
}

export const chromeStyles = styles;
