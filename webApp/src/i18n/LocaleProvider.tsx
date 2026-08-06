import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { en } from './en';
import { ar } from './ar';

export type Lang = 'en' | 'ar';
export type StringKey = keyof typeof en;

const STORAGE_KEY = 'thykra.lang';
const DICTS: Record<Lang, Record<string, string>> = { en, ar };

interface LocaleContextType {
  lang: Lang;
  dir: 'ltr' | 'rtl';
  setLang: (lang: Lang) => void;
  /** Translate a key, interpolating `{name}` placeholders from params. */
  t: (key: StringKey, params?: Record<string, string | number>) => string;
}

const LocaleContext = createContext<LocaleContextType | null>(null);

function readStoredLang(): Lang {
  try {
    const v = localStorage.getItem(STORAGE_KEY);
    if (v === 'ar') return 'ar';
  } catch {
    /* private mode */
  }
  return 'en';
}

function applyLang(lang: Lang) {
  document.documentElement.setAttribute('lang', lang);
  document.documentElement.setAttribute('dir', lang === 'ar' ? 'rtl' : 'ltr');
}

export function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (m, name) =>
    name in params ? String(params[name]) : m,
  );
}

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(readStoredLang);

  useEffect(() => {
    applyLang(lang);
  }, [lang]);

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* private mode */
    }
  }, []);

  const t = useCallback(
    (key: StringKey, params?: Record<string, string | number>) => {
      const dict = DICTS[lang];
      const template = dict[key] ?? en[key] ?? key;
      return interpolate(template, params);
    },
    [lang],
  );

  const value = useMemo<LocaleContextType>(
    () => ({ lang, dir: lang === 'ar' ? 'rtl' : 'ltr', setLang, t }),
    [lang, setLang, t],
  );

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale(): LocaleContextType {
  const ctx = useContext(LocaleContext);
  if (!ctx) throw new Error('useLocale must be used within LocaleProvider');
  return ctx;
}

export function useT() {
  return useLocale().t;
}
