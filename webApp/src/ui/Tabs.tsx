import { useId, type ReactNode } from 'react';
import styles from './Tabs.module.css';

/**
 * Tabs navigate routes; render <a>/<Link> children carrying
 * aria-current="page" when active — arrow keys are not hijacked because
 * these are links, not a tablist.
 */
export function Tabs({ children, label, end }: { children: ReactNode; label: string; end?: ReactNode }) {
  return (
    <nav className={styles.tabs} aria-label={label}>
      {children}
      {end}
    </nav>
  );
}

export const tabClass = styles.tab;

export interface SegmentOption<V extends string> {
  value: V;
  label: string;
  /** A segment with a count of 0 is disabled, not hidden. */
  disabled?: boolean;
}

/**
 * Segmented switches a view in place. role="radiogroup" with real radios
 * visually hidden, so arrow keys move the selection natively.
 */
export function Segmented<V extends string>({
  options,
  value,
  onChange,
  label,
  fullWidthMobile,
}: {
  options: SegmentOption<V>[];
  value: V;
  onChange: (value: V) => void;
  label: string;
  fullWidthMobile?: boolean;
}) {
  const name = useId();
  return (
    <div
      className={[styles.segmented, fullWidthMobile ? styles.fullWidthMobile : '']
        .filter(Boolean)
        .join(' ')}
      role="radiogroup"
      aria-label={label}
    >
      {options.map((opt) => (
        <label key={opt.value} className={styles.segment}>
          <input
            type="radio"
            className={styles.segmentInput}
            name={name}
            value={opt.value}
            checked={value === opt.value}
            disabled={opt.disabled}
            onChange={() => onChange(opt.value)}
          />
          <span className={styles.segmentLabel}>{opt.label}</span>
        </label>
      ))}
    </div>
  );
}
