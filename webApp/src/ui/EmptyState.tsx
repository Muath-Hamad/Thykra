import type { ReactNode } from 'react';
import styles from './EmptyState.module.css';

/**
 * Empty is not error. Empty uses the dashed blank-plate device; error uses
 * the same layout with --bad-soft behind the plate and Retry as the primary
 * action. Both announce with role="status". Every empty state names the
 * next action — "No data" alone is never shipped.
 */
export function EmptyState({
  kind = 'empty',
  plateLabel,
  title,
  body,
  actions,
}: {
  kind?: 'empty' | 'error';
  plateLabel?: string;
  title: string;
  body?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <div className={styles.wrap} role="status">
      <div className={[styles.plate, kind === 'error' ? styles.plateError : ''].filter(Boolean).join(' ')}>
        {plateLabel && <span className={styles.plateLabel}>{plateLabel}</span>}
      </div>
      <div className={styles.title}>{title}</div>
      {body && <p className={styles.body}>{body}</p>}
      {actions && <div className={styles.actions}>{actions}</div>}
    </div>
  );
}
