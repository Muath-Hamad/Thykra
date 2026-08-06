import { useEffect, useState, type CSSProperties, type ReactNode } from 'react';
import styles from './Skeleton.module.css';

/**
 * A skeleton mirrors the real box it replaces. Radius matches the target —
 * 0 for plates. Appears only after 150ms of waiting so fast responses
 * never flash (use <Delayed>).
 */
export function Skeleton({
  width,
  height,
  radius = 4,
  aspectRatio,
  className,
  style,
}: {
  width?: number | string;
  height?: number | string;
  radius?: number | string;
  aspectRatio?: string;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <div
      className={[styles.skeleton, className ?? ''].filter(Boolean).join(' ')}
      style={{ width, height, borderRadius: radius, aspectRatio, ...style }}
      aria-hidden="true"
    />
  );
}

/** Renders children only after `ms` (default 150) — the skeleton delay. */
export function Delayed({ ms = 150, children }: { ms?: number; children: ReactNode }) {
  const [show, setShow] = useState(false);
  useEffect(() => {
    const timer = window.setTimeout(() => setShow(true), ms);
    return () => window.clearTimeout(timer);
  }, [ms]);
  return show ? <>{children}</> : null;
}

/** List skeleton — 3 rows max, never a full page. */
export function ListSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className={styles.listRows} aria-hidden="true">
      {Array.from({ length: Math.min(rows, 3) }, (_, i) => (
        <div key={i} className={styles.listRow}>
          <Skeleton width={32} height={32} radius="50%" />
          <div style={{ flex: 1, display: 'grid', gap: 7 }}>
            <Skeleton width={`${[52, 66, 44][i % 3]}%`} height={12} />
            <Skeleton width={`${[32, 28, 36][i % 3]}%`} height={10} />
          </div>
        </div>
      ))}
    </div>
  );
}
