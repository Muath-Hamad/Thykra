import { useEffect, useRef, useState } from 'react';
import {
  MediaReactionsDto,
  REACTION_EMOJI,
  REACTION_LABEL,
  REACTION_TYPES,
  ReactionType,
} from '../api/reactions';

interface ReactionBarProps {
  data: MediaReactionsDto | null;
  loading: boolean;
  onToggle: (type: ReactionType) => void;
}

export function ReactionBar({ data, loading, onToggle }: ReactionBarProps) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!pickerOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setPickerOpen(false);
      }
    }
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        e.stopPropagation();
        setPickerOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKey, true);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKey, true);
    };
  }, [pickerOpen]);

  function handleSelect(type: ReactionType) {
    onToggle(type);
    setPickerOpen(false);
  }

  const summary = data?.summary ?? [];
  const visible = summary.filter((s) => s.count > 0);

  return (
    <>
      <style>{`
        .reaction-bar {
          position: relative;
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 0.4rem;
        }

        .reaction-pill {
          display: inline-flex;
          align-items: center;
          gap: 0.3rem;
          padding: 0.32rem 0.7rem;
          background: rgba(253, 248, 239, 0.1);
          border: 1px solid rgba(253, 248, 239, 0.18);
          color: var(--color-warm-white);
          border-radius: var(--radius-full);
          font-family: var(--font-body);
          font-size: 0.8rem;
          font-weight: 500;
          cursor: pointer;
          transition: background 0.15s, border-color 0.15s, transform 0.15s;
          line-height: 1;
        }
        .reaction-pill:hover {
          background: rgba(253, 248, 239, 0.18);
          transform: translateY(-1px);
        }
        .reaction-pill:disabled {
          opacity: 0.6;
          cursor: not-allowed;
          transform: none;
        }
        .reaction-pill.active {
          background: rgba(27, 127, 204, 0.32);
          border-color: var(--color-ocean-blue);
        }
        .reaction-pill .pill-emoji {
          font-size: 1rem;
          line-height: 1;
        }
        .reaction-pill .pill-count {
          font-variant-numeric: tabular-nums;
          font-size: 0.78rem;
        }

        .reaction-add-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          background: rgba(253, 248, 239, 0.1);
          border: 1px dashed rgba(253, 248, 239, 0.32);
          color: var(--color-warm-white);
          border-radius: 50%;
          font-size: 1rem;
          cursor: pointer;
          transition: background 0.15s, border-color 0.15s;
        }
        .reaction-add-btn:hover {
          background: rgba(253, 248, 239, 0.2);
          border-color: rgba(253, 248, 239, 0.5);
        }

        .reaction-picker {
          position: absolute;
          bottom: calc(100% + 0.5rem);
          left: 0;
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          border: 1px solid rgba(27, 127, 204, 0.16);
          border-radius: var(--radius-md);
          box-shadow: var(--shadow-lg);
          padding: 0.5rem;
          display: grid;
          grid-template-columns: repeat(4, 40px);
          gap: 0.25rem;
          z-index: 1100;
          animation: reactionPopIn 0.14s ease-out;
        }
        @keyframes reactionPopIn {
          from { opacity: 0; transform: translateY(4px) scale(0.96); }
          to   { opacity: 1; transform: translateY(0) scale(1); }
        }

        .reaction-option {
          width: 40px;
          height: 40px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: transparent;
          border: none;
          border-radius: var(--radius-sm);
          font-size: 1.4rem;
          cursor: pointer;
          transition: background 0.12s, transform 0.12s;
          line-height: 1;
        }
        .reaction-option:hover {
          background: var(--color-sandy);
          transform: scale(1.18);
        }
        .reaction-option.active {
          background: rgba(27, 127, 204, 0.14);
          box-shadow: inset 0 0 0 2px var(--color-sky-blue);
        }
      `}</style>

      <div className="reaction-bar" ref={containerRef}>
        {visible.map((s) => (
          <button
            key={s.type}
            className={`reaction-pill ${s.reactedByMe ? 'active' : ''}`}
            onClick={() => onToggle(s.type)}
            disabled={loading}
            title={REACTION_LABEL[s.type]}
            aria-label={`${REACTION_LABEL[s.type]} ${s.count}${s.reactedByMe ? ' (you reacted)' : ''}`}
          >
            <span className="pill-emoji">{REACTION_EMOJI[s.type]}</span>
            <span className="pill-count">{s.count}</span>
          </button>
        ))}

        <button
          className="reaction-add-btn"
          onClick={() => setPickerOpen((v) => !v)}
          aria-haspopup="menu"
          aria-expanded={pickerOpen}
          aria-label="Add a reaction"
          title="Add a reaction"
        >
          {pickerOpen ? '×' : '+'}
        </button>

        {pickerOpen && (
          <div className="reaction-picker" role="menu">
            {REACTION_TYPES.map((t) => {
              const isActive = summary.find((s) => s.type === t)?.reactedByMe ?? false;
              return (
                <button
                  key={t}
                  className={`reaction-option ${isActive ? 'active' : ''}`}
                  onClick={() => handleSelect(t)}
                  role="menuitem"
                  title={REACTION_LABEL[t]}
                  aria-label={REACTION_LABEL[t]}
                >
                  {REACTION_EMOJI[t]}
                </button>
              );
            })}
          </div>
        )}
      </div>
    </>
  );
}
