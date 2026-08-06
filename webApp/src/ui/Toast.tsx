import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import styles from './Toast.module.css';

export type ToastKind = 'success' | 'info' | 'warm' | 'error';

export interface ToastOptions {
  kind?: ToastKind;
  title: string;
  body?: string;
  /** Undo / Retry — extends auto-dismiss to 6s; errors never auto-dismiss. */
  action?: { label: string; onClick: () => void; outline?: boolean };
  /** Leading spinner (upload progress toast). */
  spinner?: boolean;
  /** Override auto-dismiss (ms); 0 disables. */
  duration?: number;
}

interface ToastItem extends ToastOptions {
  id: number;
  leaving: boolean;
}

interface ToastContextType {
  toast: (options: ToastOptions) => number;
  dismiss: (id: number) => void;
}

const ToastContext = createContext<ToastContextType | null>(null);

let nextToastId = 1;

const MAX_STACK = 3;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const timers = useRef(new Map<number, { handle: number; remaining: number; startedAt: number }>());

  const remove = useCallback((id: number) => {
    const timer = timers.current.get(id);
    if (timer) window.clearTimeout(timer.handle);
    timers.current.delete(id);
    setItems((prev) => prev.map((t) => (t.id === id ? { ...t, leaving: true } : t)));
    window.setTimeout(() => {
      setItems((prev) => prev.filter((t) => t.id !== id));
    }, 200);
  }, []);

  const schedule = useCallback(
    (id: number, ms: number) => {
      if (ms <= 0) return;
      const handle = window.setTimeout(() => remove(id), ms);
      timers.current.set(id, { handle, remaining: ms, startedAt: Date.now() });
    },
    [remove],
  );

  const toast = useCallback(
    (options: ToastOptions): number => {
      const id = nextToastId++;
      const kind = options.kind ?? 'info';
      const duration =
        options.duration !== undefined
          ? options.duration
          : kind === 'error'
            ? 0 // never auto-dismiss an error
            : options.action
              ? 6000
              : 4000;
      setItems((prev) => {
        const next = [...prev, { ...options, kind, id, leaving: false }];
        // Max 3 stacked — oldest non-error goes first.
        if (next.filter((t) => !t.leaving).length > MAX_STACK) {
          const evict = next.find((t) => !t.leaving && t.kind !== 'error') ?? next[0];
          window.setTimeout(() => remove(evict.id), 0);
        }
        return next;
      });
      schedule(id, duration);
      return id;
    },
    [remove, schedule],
  );

  // Hover or focus pauses the timer; it RESTARTS (full window) on leave.
  const pause = useCallback((id: number) => {
    const timer = timers.current.get(id);
    if (!timer) return;
    window.clearTimeout(timer.handle);
  }, []);

  const resume = useCallback(
    (id: number) => {
      const timer = timers.current.get(id);
      if (!timer) return;
      schedule(id, timer.remaining);
    },
    [schedule],
  );

  const value = useMemo(() => ({ toast, dismiss: remove }), [toast, remove]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {createPortal(
        <div className={styles.viewport}>
          {/* One persistent polite live region; errors are announced assertively. */}
          <div aria-live="polite" className="sr-only">
            {items
              .filter((t) => !t.leaving && t.kind !== 'error')
              .map((t) => `${t.title}${t.body ? `. ${t.body}` : ''}`)
              .join(' ')}
          </div>
          <div aria-live="assertive" className="sr-only">
            {items
              .filter((t) => !t.leaving && t.kind === 'error')
              .map((t) => `${t.title}${t.body ? `. ${t.body}` : ''}`)
              .join(' ')}
          </div>
          {items.map((t) => (
            <div
              key={t.id}
              className={[styles.toast, styles[t.kind ?? 'info'], t.leaving ? styles.leaving : '']
                .filter(Boolean)
                .join(' ')}
              onMouseEnter={() => pause(t.id)}
              onMouseLeave={() => resume(t.id)}
              onFocus={() => pause(t.id)}
              onBlur={() => resume(t.id)}
            >
              {t.spinner && <span className={styles.spinner} aria-hidden="true" />}
              <div className={styles.content}>
                <div className={styles.title}>{t.title}</div>
                {t.body && <div className={styles.body}>{t.body}</div>}
              </div>
              {t.action && (
                <button
                  className={t.action.outline ? styles.actionOutline : styles.action}
                  onClick={() => {
                    t.action!.onClick();
                    remove(t.id);
                  }}
                >
                  {t.action.label}
                </button>
              )}
              <button className={styles.dismiss} onClick={() => remove(t.id)} aria-label="Dismiss">
                ✕
              </button>
            </div>
          ))}
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextType {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
