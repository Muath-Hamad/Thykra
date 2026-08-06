import type { ReactNode } from 'react';
import styles from './Stamp.module.css';

export type StampTone = 'warm' | 'ink' | 'warn';

export function Stamp({
  eyebrow,
  name,
  avatar,
  tone = 'warm',
}: {
  eyebrow?: string;
  name: string;
  avatar?: ReactNode;
  tone?: StampTone;
}) {
  return (
    <span
      className={[styles.stamp, tone === 'ink' ? styles.ink : '', tone === 'warn' ? styles.warn : '']
        .filter(Boolean)
        .join(' ')}
    >
      <span className={styles.inner}>
        {avatar}
        <span className={styles.text}>
          {eyebrow && <span className={styles.eyebrow}>{eyebrow}</span>}
          <span className={styles.name}>{name}</span>
        </span>
      </span>
    </span>
  );
}
