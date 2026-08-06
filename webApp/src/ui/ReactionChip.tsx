import { useRef, useState } from 'react';
import { REACTION_EMOJI, type ReactionType } from '../api/reactions';
import { useT } from '../i18n/LocaleProvider';
import styles from './ReactionChip.module.css';

export function ReactionChip({
  type,
  count,
  mine,
  onToggle,
  onDark,
  disabled,
}: {
  type: ReactionType;
  count: number;
  mine: boolean;
  onToggle?: (type: ReactionType) => void;
  onDark?: boolean;
  disabled?: boolean;
}) {
  const t = useT();
  const [burst, setBurst] = useState(0);
  const [particle, setParticle] = useState(0);
  const wasMine = useRef(mine);

  const handleClick = () => {
    if (!onToggle) return;
    if (!wasMine.current) {
      // Adding a reaction — the one exuberant moment in the product.
      setBurst((b) => b + 1);
      setParticle((p) => p + 1);
    }
    wasMine.current = !wasMine.current;
    onToggle(type);
  };

  const label = t(`reaction.${type}` as never);
  return (
    <button
      type="button"
      className={[styles.chip, mine ? styles.mine : '', onDark ? styles.onDark : '']
        .filter(Boolean)
        .join(' ')}
      onClick={handleClick}
      disabled={disabled}
      aria-pressed={mine}
      aria-label={`${label}${count > 0 ? ` — ${count}` : ''}`}
    >
      <span key={burst} className={burst > 0 ? styles.burst : undefined} aria-hidden="true">
        {REACTION_EMOJI[type]}
      </span>
      {count > 0 && <span className={styles.count}>{count}</span>}
      {particle > 0 && (
        <span key={`p${particle}`} className={styles.particle} aria-hidden="true">
          {REACTION_EMOJI[type]}
        </span>
      )}
    </button>
  );
}
