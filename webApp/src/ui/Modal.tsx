import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ReactNode,
  type TouchEvent as ReactTouchEvent,
} from 'react';
import { createPortal } from 'react-dom';
import { useT } from '../i18n/LocaleProvider';
import { Button } from './Button';
import { Input } from './Field';
import styles from './Modal.module.css';

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

let scrollLocks = 0;

function lockScroll() {
  if (scrollLocks++ === 0) {
    const scrollbar = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.overflow = 'hidden';
    if (scrollbar > 0) document.body.style.paddingInlineEnd = `${scrollbar}px`;
  }
}

function unlockScroll() {
  if (--scrollLocks === 0) {
    document.body.style.overflow = '';
    document.body.style.paddingInlineEnd = '';
  }
}

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  /** Micro-label above the title ("New trip"). */
  eyebrow?: string;
  title: string;
  /** Hide the header (Confirm draws its own). */
  hideHeader?: boolean;
  /** 560px instead of 480px. */
  wide?: boolean;
  /** While true, Esc and scrim-click are disabled — a submit is in flight. */
  busy?: boolean;
  /** One-line request error rendered above the footer in --bad-soft. */
  error?: string | null;
  children?: ReactNode;
  footer?: ReactNode;
  /** Move initial focus to this element instead of the first focusable. */
  initialFocusRef?: React.RefObject<HTMLElement | null>;
}

export function Modal({
  open,
  onClose,
  eyebrow,
  title,
  hideHeader,
  wide,
  busy,
  error,
  children,
  footer,
  initialFocusRef,
}: ModalProps) {
  const dialogRef = useRef<HTMLDivElement | null>(null);
  const previousFocus = useRef<HTMLElement | null>(null);
  const busyRef = useRef(!!busy);
  busyRef.current = !!busy;

  // Focus management: first interactive element on open; return on close.
  useLayoutEffect(() => {
    if (!open) return;
    previousFocus.current = document.activeElement as HTMLElement | null;
    lockScroll();
    const el =
      initialFocusRef?.current ??
      dialogRef.current?.querySelector<HTMLElement>(FOCUSABLE) ??
      dialogRef.current;
    el?.focus();
    return () => {
      unlockScroll();
      previousFocus.current?.focus?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const onKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        if (!busyRef.current) onClose();
        return;
      }
      if (e.key !== 'Tab') return;
      const dialog = dialogRef.current;
      if (!dialog) return;
      const focusables = [...dialog.querySelectorAll<HTMLElement>(FOCUSABLE)].filter(
        (el) => el.offsetParent !== null || el === document.activeElement,
      );
      if (focusables.length === 0) return;
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    },
    [onClose],
  );

  // Sheet drag-to-dismiss (<768px): follows the finger, snaps closed past
  // 40% height or 0.5px/ms velocity. Direct manipulation — works under
  // reduced motion too.
  const dragState = useRef<{ startY: number; startT: number; lastY: number } | null>(null);
  const [dragOffset, setDragOffset] = useState(0);

  const onTouchStart = (e: ReactTouchEvent) => {
    if (window.innerWidth >= 768) return;
    const body = dialogRef.current?.querySelector(`.${styles.body}`);
    if (body && body.scrollTop > 0) return;
    dragState.current = {
      startY: e.touches[0].clientY,
      startT: performance.now(),
      lastY: e.touches[0].clientY,
    };
  };
  const onTouchMove = (e: ReactTouchEvent) => {
    if (!dragState.current) return;
    const dy = e.touches[0].clientY - dragState.current.startY;
    dragState.current.lastY = e.touches[0].clientY;
    if (dy > 0) setDragOffset(dy);
  };
  const onTouchEnd = () => {
    const state = dragState.current;
    dragState.current = null;
    if (!state) return;
    const dy = state.lastY - state.startY;
    const dt = performance.now() - state.startT;
    const height = dialogRef.current?.getBoundingClientRect().height ?? 1;
    if (!busyRef.current && (dy > height * 0.4 || (dt > 0 && dy / dt > 0.5))) {
      onClose();
    }
    setDragOffset(0);
  };

  useEffect(() => {
    if (!open) setDragOffset(0);
  }, [open]);

  if (!open) return null;

  return createPortal(
    <div
      className={styles.scrim}
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busyRef.current) onClose();
      }}
    >
      <div
        ref={dialogRef}
        className={[styles.dialog, wide ? styles.wide : ''].filter(Boolean).join(' ')}
        role="dialog"
        aria-modal="true"
        aria-label={hideHeader ? title : undefined}
        aria-labelledby={hideHeader ? undefined : 'modal-title'}
        onKeyDown={onKeyDown}
        tabIndex={-1}
        style={dragOffset ? { transform: `translateY(${dragOffset}px)`, transition: 'none' } : undefined}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
      >
        <div className={styles.handle} aria-hidden="true" />
        {!hideHeader && (
          <div className={styles.header}>
            {eyebrow && <div className={styles.eyebrow}>{eyebrow}</div>}
            <div className={styles.title} id="modal-title">
              {title}
            </div>
          </div>
        )}
        {children}
        {error && <div className={styles.errorStrip} role="alert">{error}</div>}
        {footer && <div className={styles.footer}>{footer}</div>}
      </div>
    </div>,
    document.body,
  );
}

export function ModalBody({ children }: { children: ReactNode }) {
  return <div className={styles.body}>{children}</div>;
}

export interface ConfirmProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  /** A question naming the object: "Remove Omar from Wadi Rum?" */
  title: string;
  body: ReactNode;
  confirmLabel: string;
  cancelLabel: string;
  busy?: boolean;
  error?: string | null;
  danger?: boolean;
  /** Deleting a trip requires typing the exact title. */
  typeToConfirm?: string;
  typeToConfirmLabel?: string;
  typeToConfirmPlaceholder?: string;
}

export function Confirm({
  open,
  onClose,
  onConfirm,
  title,
  body,
  confirmLabel,
  cancelLabel,
  busy,
  error,
  danger = true,
  typeToConfirm,
  typeToConfirmLabel,
  typeToConfirmPlaceholder,
}: ConfirmProps) {
  const t = useT();
  const [typed, setTyped] = useState('');
  const cancelRef = useRef<HTMLButtonElement | null>(null);
  useEffect(() => {
    if (!open) setTyped('');
  }, [open]);

  const confirmDisabled = !!typeToConfirm && typed !== typeToConfirm;

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      hideHeader
      busy={busy}
      error={error}
      initialFocusRef={cancelRef as React.RefObject<HTMLElement>}
    >
      <div className={styles.confirmPad}>
        {danger && (
          <div className={styles.confirmIcon} aria-hidden="true">
            !
          </div>
        )}
        <div className={styles.confirmTitle}>{title}</div>
        <div className={styles.confirmBody}>{body}</div>
        {typeToConfirm && (
          <div style={{ marginTop: 14 }}>
            <Input
              label={typeToConfirmLabel ?? t('settings.delete.placeholder', { title: typeToConfirm })}
              placeholder={typeToConfirmPlaceholder}
              value={typed}
              onChange={(e) => setTyped(e.target.value)}
              autoComplete="off"
            />
          </div>
        )}
        <div className={styles.confirmActions}>
          <Button ref={cancelRef as never} variant="ghost" onClick={onClose} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button
            variant={danger ? 'danger' : 'primary'}
            onClick={() => void onConfirm()}
            disabled={confirmDisabled}
            loading={busy}
            loadingLabel={confirmLabel}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
