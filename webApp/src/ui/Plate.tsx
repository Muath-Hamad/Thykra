import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import { Play } from 'lucide-react';
import styles from './Plate.module.css';

/**
 * PlateImage — loads src, fires the 520ms plate-resolve once on decode.
 * Skipped for anything already in the browser cache (decode resolves
 * before the first paint → no animation class).
 */
export function PlateImage({
  src,
  alt = '',
  eager,
}: {
  src: string;
  alt?: string;
  eager?: boolean;
}) {
  const ref = useRef<HTMLImageElement | null>(null);
  const [resolved, setResolved] = useState(false);
  const [animate, setAnimate] = useState(false);

  useEffect(() => {
    const img = ref.current;
    if (!img) return;
    let cancelled = false;
    setResolved(false);
    setAnimate(false);
    const started = performance.now();
    const onReady = () => {
      if (cancelled) return;
      setResolved(true);
      // Cached images decode in a frame or two — skip the flourish.
      setAnimate(performance.now() - started > 50);
    };
    if (img.complete && img.naturalWidth > 0) {
      onReady();
    } else {
      img.addEventListener('load', onReady);
      img.addEventListener('error', onReady);
      return () => {
        cancelled = true;
        img.removeEventListener('load', onReady);
        img.removeEventListener('error', onReady);
      };
    }
    return () => {
      cancelled = true;
    };
  }, [src]);

  return (
    <img
      ref={ref}
      src={src}
      alt={alt}
      loading={eager ? 'eager' : 'lazy'}
      className={animate ? 'plate-resolve' : undefined}
      style={resolved ? undefined : { opacity: 0 }}
    />
  );
}

export interface PlateProps {
  src: string;
  alt: string;
  aspectRatio?: string;
  isVideo?: boolean;
  durationLabel?: string;
  /** Corner chips: "❤️ 4", "💬 2". */
  chips?: string[];
  onClick?: () => void;
  style?: CSSProperties;
  className?: string;
  tabIndex?: number;
  eager?: boolean;
  children?: ReactNode;
  buttonRef?: (el: HTMLButtonElement | null) => void;
  ariaLabel?: string;
}

/** A media plate. Interactive plates are real <button>s, never click-divs. */
export function Plate({
  src,
  alt,
  aspectRatio,
  isVideo,
  durationLabel,
  chips,
  onClick,
  style,
  className,
  tabIndex,
  eager,
  children,
  buttonRef,
  ariaLabel,
}: PlateProps) {
  const inner = (
    <>
      <PlateImage src={src} alt={onClick ? '' : alt} eager={eager} />
      {isVideo && (
        <span className={styles.playGlyph}>
          <Play size={28} fill="currentColor" strokeWidth={0} aria-hidden="true" />
        </span>
      )}
      {durationLabel && <span className={styles.duration}>{durationLabel}</span>}
      {chips && chips.length > 0 && (
        <span className={styles.chips}>
          {chips.map((c, i) => (
            <span key={i} className={styles.chip}>
              {c}
            </span>
          ))}
        </span>
      )}
      {children}
    </>
  );

  if (onClick) {
    return (
      <button
        type="button"
        ref={buttonRef}
        className={[styles.plate, styles.interactive, className ?? ''].filter(Boolean).join(' ')}
        style={{ aspectRatio, ...style }}
        onClick={onClick}
        tabIndex={tabIndex}
        aria-label={ariaLabel ?? alt}
      >
        {inner}
      </button>
    );
  }
  return (
    <div
      className={[styles.plate, className ?? ''].filter(Boolean).join(' ')}
      style={{ aspectRatio, ...style }}
    >
      {inner}
    </div>
  );
}
