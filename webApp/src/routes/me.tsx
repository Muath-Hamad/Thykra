import { useEffect, useRef, useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { apiClient } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useLocale, type Lang } from '../i18n/LocaleProvider';
import { useTheme, type ThemeChoice } from '../theme/ThemeProvider';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Input } from '../ui/Field';
import { Segmented } from '../ui/Tabs';
import { useToast } from '../ui/Toast';
import { useDocumentTitle } from '../ui/hooks';
import styles from './me.module.css';

/**
 * /me — short by design. Identity, one editable field, the two preferences
 * the redesign introduces, and sign out. Nothing that has a route of its own.
 */
export function MePage() {
  const { t, lang, setLang } = useLocale();
  const { user, logout } = useAuth();
  const { choice, resolved, setChoice } = useTheme();
  const { toast } = useToast();
  const navigate = useNavigate();

  // AuthContext exposes no setter for the signed-in user, so the saved name
  // is mirrored locally for this screen only.
  const [nameOverride, setNameOverride] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement | null>(null);

  useDocumentTitle(`${t('nav.you')} · Thykra`);

  const serverName = user?.displayName ?? '';
  const displayName = nameOverride ?? serverName;

  useEffect(() => {
    setDraft(serverName);
  }, [serverName]);

  // Focus lands on the name field only when the route is opened with #name.
  useEffect(() => {
    if (window.location.hash === '#name') inputRef.current?.focus();
  }, []);

  if (!user) return null;

  const dirty = draft.trim() !== displayName;

  const save = async () => {
    const next = draft.trim();
    if (!next) {
      setError(t('me.displayNameRequired'));
      inputRef.current?.focus();
      return;
    }
    setError(null);
    const previous = displayName;
    // Optimistic: the new name is on screen before the request lands.
    setNameOverride(next);
    setSaving(true);
    const resp = await apiClient('/api/profile', {
      method: 'PUT',
      body: JSON.stringify({ displayName: next }),
    }).catch(() => null);
    setSaving(false);
    if (resp?.success) {
      toast({ kind: 'success', title: t('me.saved') });
    } else {
      // Rollback plus a toast — the field never lies about what was stored.
      setNameOverride(previous === serverName ? null : previous);
      setDraft(previous);
      toast({ kind: 'error', title: t('me.saveFailed') });
    }
  };

  const signOut = () => {
    logout();
    void navigate({ to: '/' });
  };

  return (
    <div className={`page-enter ${styles.page}`}>
      {/* ── Identity ── */}
      <section className={`${styles.section} ${styles.identity}`}>
        <Avatar
          userId={user.id}
          displayName={displayName}
          avatarUrl={user.avatarUrl}
          size="xl"
        />
        <div className={styles.identityText}>
          <h1 className="t-display-s">{displayName}</h1>
          <p className="t-meta">{user.email}</p>
        </div>
      </section>

      {/* ── Display name ── */}
      <section className={styles.section} id="name">
        <div className={styles.nameRow}>
          <div className={styles.nameField}>
            <Input
              ref={inputRef}
              label={t('me.displayName')}
              help={t('me.displayNameHelp')}
              error={error}
              value={draft}
              onChange={(e) => {
                setDraft(e.target.value);
                if (error) setError(null);
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && dirty) {
                  e.preventDefault();
                  void save();
                }
              }}
            />
          </div>
          {/* Button spreads rest props last, so the class lives on a wrapper. */}
          <span className={styles.saveButton}>
            <Button
              onClick={() => void save()}
              disabled={!dirty}
              loading={saving}
              loadingLabel={t('common.saving')}
            >
              {t('common.save')}
            </Button>
          </span>
        </div>
      </section>

      {/* ── Email — disabled, with the reason stated ── */}
      <section className={styles.section}>
        <Input label={t('me.email')} help={t('me.emailHelp')} value={user.email} disabled readOnly />
      </section>

      {/* ── Appearance ── */}
      <section className={styles.section}>
        <span className={`t-label ${styles.groupLabel}`}>{t('me.appearance')}</span>
        <Segmented<ThemeChoice>
          label={t('me.appearance')}
          options={[
            { value: 'system', label: t('me.appearance.system') },
            { value: 'light', label: t('me.appearance.paper') },
            { value: 'dark', label: t('me.appearance.darkroom') },
          ]}
          value={choice}
          onChange={setChoice}
          fullWidthMobile
        />
        {choice === 'system' && (
          <p className={`t-meta ${styles.caption}`}>
            {t('me.appearance.following', {
              mode:
                resolved === 'dark'
                  ? t('me.appearance.darkroom')
                  : t('me.appearance.paper'),
            })}
          </p>
        )}
      </section>

      {/* ── Language ── */}
      <section className={styles.section}>
        <span className={`t-label ${styles.groupLabel}`}>{t('me.language')}</span>
        <Segmented<Lang>
          label={t('me.language')}
          options={[
            { value: 'en', label: t('me.language.en') },
            { value: 'ar', label: t('me.language.ar') },
          ]}
          value={lang}
          onChange={setLang}
          fullWidthMobile
        />
      </section>

      {/* ── Sign out — secondary, because it is not a primary action ── */}
      <section className={`${styles.section} ${styles.signOutRow}`}>
        <Button variant="secondary" onClick={signOut}>
          {t('nav.signOut')}
        </Button>
        <span className="t-meta">{t('me.signedInWith')}</span>
      </section>
    </div>
  );
}
