import type { ReactNode } from 'react';
import styles from './Badge.module.css';

export type BadgeKind =
  | 'owner'
  | 'contributor'
  | 'viewer'
  | 'ink'
  | 'blocked'
  | 'warm'
  | 'good';

export function Badge({ kind, children }: { kind: BadgeKind; children: ReactNode }) {
  return <span className={`${styles.badge} ${styles[kind]}`}>{children}</span>;
}
