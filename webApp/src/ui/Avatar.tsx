import { useState, type ButtonHTMLAttributes } from 'react';
import styles from './Avatar.module.css';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

const SIZES: Record<AvatarSize, number> = {
  xs: 20,
  sm: 26,
  md: 32,
  lg: 40,
  xl: 56,
  '2xl': 88,
};

const FONT: Record<AvatarSize, number> = {
  xs: 9,
  sm: 11,
  md: 13,
  lg: 15,
  xl: 20,
  '2xl': 32,
};

/**
 * Five ink-compatible tints. The fill is chosen by hashing userId, so the
 * same person is the same colour on every screen — which is the whole point.
 */
export const AVATAR_TINTS = ['#AA4324', '#1B6FBE', '#4E5A69', '#1F7A4D', '#8A5D12'] as const;

export function tintFor(userId: string): string {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = (hash * 31 + userId.charCodeAt(i)) | 0;
  }
  return AVATAR_TINTS[Math.abs(hash) % AVATAR_TINTS.length];
}

export function initialsOf(displayName: string): string {
  const parts = displayName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '·';
  return parts[0].charAt(0).toUpperCase();
}

export interface AvatarProps {
  userId: string;
  displayName: string;
  avatarUrl?: string | null;
  size?: AvatarSize;
  /** Decorative inside a stack — hidden from AT. */
  decorative?: boolean;
}

export function Avatar({ userId, displayName, avatarUrl, size = 'md', decorative }: AvatarProps) {
  const [failed, setFailed] = useState(false);
  const px = SIZES[size];
  const showImage = !!avatarUrl && !failed;
  return (
    <span
      className={[styles.avatar, !showImage && failed && avatarUrl ? styles.failed : '']
        .filter(Boolean)
        .join(' ')}
      style={{
        width: px,
        height: px,
        fontSize: FONT[size],
        background: showImage ? undefined : tintFor(userId),
      }}
      aria-hidden={decorative || undefined}
      role={decorative ? undefined : 'img'}
      aria-label={decorative ? undefined : displayName}
    >
      {showImage ? (
        <img src={avatarUrl!} alt="" onError={() => setFailed(true)} />
      ) : (
        initialsOf(displayName)
      )}
    </span>
  );
}

export interface StackPerson {
  userId: string;
  displayName: string;
  avatarUrl?: string | null;
}

interface AvatarStackProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  people: StackPerson[];
  /** Total member count — the +n chip is memberCount − people.length. */
  totalCount: number;
  size?: AvatarSize;
  /** Background the ring should match (defaults to var(--bg)). */
  ringColor?: string;
  /** Accessible name; defaults to the real sentence with names. */
  label?: string;
  /** Render as a plain (non-interactive) group. */
  asDecoration?: boolean;
}

export function stackLabel(people: StackPerson[], totalCount: number): string {
  const names = people.map((p) => p.displayName.split(/\s+/)[0]);
  const extra = totalCount - people.length;
  if (extra > 0) {
    return `${totalCount} people — ${names.join(', ')} and ${extra} more`;
  }
  const nameList =
    names.length > 1
      ? `${names.slice(0, -1).join(', ')} and ${names[names.length - 1]}`
      : names[0] ?? '';
  return `${totalCount} people — ${nameList}`;
}

export function AvatarStack({
  people,
  totalCount,
  size = 'md',
  ringColor,
  label,
  asDecoration,
  ...rest
}: AvatarStackProps) {
  const px = SIZES[size];
  const shown = people.slice(0, 4);
  const extra = Math.max(0, totalCount - shown.length);
  const style = ringColor ? ({ '--stack-ring': ringColor } as React.CSSProperties) : undefined;

  const content = (
    <>
      {shown.map((p) => (
        <Avatar key={p.userId} {...p} size={size} decorative />
      ))}
      {extra > 0 && (
        <span
          className={styles.more}
          style={{ width: px, height: px, fontSize: FONT[size] - 2 }}
          aria-hidden="true"
        >
          +{extra}
        </span>
      )}
    </>
  );

  if (asDecoration) {
    return (
      <span className={styles.stack} style={style} aria-hidden="true">
        {content}
      </span>
    );
  }

  return (
    <button
      type="button"
      className={styles.stack}
      style={style}
      aria-label={label ?? stackLabel(shown, totalCount)}
      {...rest}
    >
      {content}
    </button>
  );
}

export function OwnerBadge({ label = 'Own' }: { label?: string }) {
  return <span className={styles.ownerBadge}>{label}</span>;
}
