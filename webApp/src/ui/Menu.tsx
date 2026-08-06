import {
  useEffect,
  useId,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import styles from './Menu.module.css';

export interface MenuItem {
  label: ReactNode;
  onSelect: () => void;
  danger?: boolean;
  icon?: ReactNode;
}

/**
 * Overflow menu (⋯) — trigger + dropdown. Arrow keys move between items,
 * Esc closes and returns focus to the trigger, click-outside closes.
 */
export function Menu({
  trigger,
  items,
  label,
}: {
  trigger: (props: {
    ref: (el: HTMLButtonElement | null) => void;
    onClick: () => void;
    'aria-expanded': boolean;
    'aria-haspopup': 'menu';
    'aria-controls': string;
  }) => ReactNode;
  items: (MenuItem | 'separator')[];
  label: string;
}) {
  const [open, setOpen] = useState(false);
  const menuId = useId();
  const wrapRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);

  useEffect(() => {
    if (!open) return;
    itemRefs.current[0]?.focus();
    const onPointerDown = (e: PointerEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('pointerdown', onPointerDown);
    return () => document.removeEventListener('pointerdown', onPointerDown);
  }, [open]);

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.stopPropagation();
      setOpen(false);
      triggerRef.current?.focus();
      return;
    }
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      const buttons = itemRefs.current.filter(Boolean) as HTMLButtonElement[];
      const idx = buttons.indexOf(document.activeElement as HTMLButtonElement);
      const next =
        e.key === 'ArrowDown'
          ? buttons[(idx + 1) % buttons.length]
          : buttons[(idx - 1 + buttons.length) % buttons.length];
      next?.focus();
    }
  };

  let itemIndex = -1;
  return (
    <div className={styles.wrap} ref={wrapRef}>
      {trigger({
        ref: (el) => (triggerRef.current = el),
        onClick: () => setOpen((o) => !o),
        'aria-expanded': open,
        'aria-haspopup': 'menu',
        'aria-controls': menuId,
      })}
      {open && (
        <div className={styles.menu} role="menu" id={menuId} aria-label={label} onKeyDown={onKeyDown}>
          {items.map((item, i) => {
            if (item === 'separator') {
              return <div key={`sep-${i}`} className={styles.separator} role="separator" />;
            }
            itemIndex += 1;
            const idx = itemIndex;
            return (
              <button
                key={i}
                ref={(el) => (itemRefs.current[idx] = el)}
                type="button"
                role="menuitem"
                className={[styles.item, item.danger ? styles.danger : ''].filter(Boolean).join(' ')}
                onClick={() => {
                  setOpen(false);
                  item.onSelect();
                }}
              >
                {item.icon}
                {item.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
