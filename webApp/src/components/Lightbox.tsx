import { useEffect, useCallback } from 'react';
import { MediaDto } from '../api/media';

interface LightboxProps {
  media: MediaDto[];
  currentIndex: number;
  onClose: () => void;
  onNavigate: (index: number) => void;
}

export function Lightbox({ media, currentIndex, onClose, onNavigate }: LightboxProps) {
  const item = media[currentIndex];
  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < media.length - 1;

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      else if (e.key === 'ArrowLeft' && hasPrev) onNavigate(currentIndex - 1);
      else if (e.key === 'ArrowRight' && hasNext) onNavigate(currentIndex + 1);
    },
    [currentIndex, hasPrev, hasNext, onClose, onNavigate]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [handleKeyDown]);

  if (!item) return null;

  return (
    <>
      <style>{`
        .lightbox-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: rgba(26, 26, 46, 0.95);
          z-index: 1000;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .lightbox-close {
          position: absolute;
          top: 1rem;
          right: 1rem;
          width: 44px;
          height: 44px;
          background: rgba(253, 248, 239, 0.08);
          border: none;
          border-radius: 50%;
          color: var(--color-warm-white);
          font-size: 1.3rem;
          cursor: pointer;
          z-index: 1002;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: background 0.2s;
        }
        .lightbox-close:hover {
          background: rgba(253, 248, 239, 0.18);
        }

        .lightbox-counter {
          position: absolute;
          top: 1.2rem;
          left: 50%;
          transform: translateX(-50%);
          color: var(--color-warm-white);
          font-family: var(--font-body);
          font-size: 0.85rem;
          font-weight: 500;
          z-index: 1002;
          opacity: 0.8;
        }

        .lightbox-arrow {
          position: absolute;
          top: 50%;
          transform: translateY(-50%);
          width: 44px;
          height: 44px;
          background: rgba(253, 248, 239, 0.1);
          color: var(--color-warm-white);
          border: none;
          font-size: 1.5rem;
          cursor: pointer;
          border-radius: 50%;
          z-index: 1002;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: background 0.2s;
        }
        .lightbox-arrow:hover {
          background: rgba(253, 248, 239, 0.22);
        }
        .lightbox-arrow-left { left: 1rem; }
        .lightbox-arrow-right { right: 1rem; }

        .lightbox-image {
          max-width: 90vw;
          max-height: 90vh;
          object-fit: contain;
          z-index: 1001;
        }
      `}</style>

      <div className="lightbox-overlay" onClick={onClose}>
        <button className="lightbox-close" onClick={onClose}>&#x2715;</button>

        <div className="lightbox-counter">
          {currentIndex + 1} / {media.length}
        </div>

        {hasPrev && (
          <button
            className="lightbox-arrow lightbox-arrow-left"
            onClick={(e) => { e.stopPropagation(); onNavigate(currentIndex - 1); }}
          >
            &#8249;
          </button>
        )}

        <img
          className="lightbox-image"
          src={item.url}
          alt={item.filename}
          onClick={(e) => e.stopPropagation()}
        />

        {hasNext && (
          <button
            className="lightbox-arrow lightbox-arrow-right"
            onClick={(e) => { e.stopPropagation(); onNavigate(currentIndex + 1); }}
          >
            &#8250;
          </button>
        )}
      </div>
    </>
  );
}
