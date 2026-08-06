import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';

export type ThemeChoice = 'system' | 'light' | 'dark';
export type ResolvedTheme = 'light' | 'dark';

const STORAGE_KEY = 'thykra.theme';

interface ThemeContextType {
  /** What the user picked in /me — System / Paper / Darkroom. */
  choice: ThemeChoice;
  /** What is actually applied right now. */
  resolved: ResolvedTheme;
  setChoice: (choice: ThemeChoice) => void;
}

const ThemeContext = createContext<ThemeContextType | null>(null);

function readStoredChoice(): ThemeChoice {
  try {
    const v = localStorage.getItem(STORAGE_KEY);
    if (v === 'light' || v === 'dark') return v;
  } catch {
    /* private mode */
  }
  return 'system';
}

function systemPrefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function apply(resolved: ResolvedTheme) {
  if (resolved === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.removeAttribute('data-theme');
  }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [choice, setChoiceState] = useState<ThemeChoice>(readStoredChoice);
  const [resolved, setResolved] = useState<ResolvedTheme>(() =>
    readStoredChoice() === 'dark' || (readStoredChoice() === 'system' && systemPrefersDark())
      ? 'dark'
      : 'light',
  );

  useEffect(() => {
    const next: ResolvedTheme =
      choice === 'dark' || (choice === 'system' && systemPrefersDark()) ? 'dark' : 'light';
    setResolved(next);
    apply(next);

    if (choice !== 'system') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => {
      const r: ResolvedTheme = e.matches ? 'dark' : 'light';
      setResolved(r);
      apply(r);
    };
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [choice]);

  const setChoice = useCallback((next: ThemeChoice) => {
    setChoiceState(next);
    try {
      if (next === 'system') localStorage.removeItem(STORAGE_KEY);
      else localStorage.setItem(STORAGE_KEY, next);
    } catch {
      /* private mode */
    }
  }, []);

  return (
    <ThemeContext.Provider value={{ choice, resolved, setChoice }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextType {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}

/**
 * Forces Paper while mounted — the marketing landing ships light-only.
 * Restores the resolved theme on unmount.
 */
export function usePaperOnly() {
  const { resolved } = useTheme();
  useEffect(() => {
    document.documentElement.removeAttribute('data-theme');
    return () => apply(resolved);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resolved]);
}
