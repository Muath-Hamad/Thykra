import { useEffect, useCallback, useState } from 'react';
import { MediaDto } from '../api/media';
import {
  MediaReactionsDto,
  ReactionType,
  getReactions,
  toggleReaction,
} from '../api/reactions';
import { ReactionBar } from './ReactionBar';
import { CommentThread } from './CommentThread';

interface LightboxProps {
  media: MediaDto[];
  currentIndex: number;
  albumId: string;
  currentUserId: string | null;
  albumOwnerId: string | null;
  onClose: () => void;
  onNavigate: (index: number) => void;
}

export function Lightbox({
  media,
  currentIndex,
  albumId,
  currentUserId,
  albumOwnerId,
  onClose,
  onNavigate,
}: LightboxProps) {
  const item = media[currentIndex];
  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < media.length - 1;

  const [reactions, setReactions] = useState<MediaReactionsDto | null>(null);
  const [reactionsLoading, setReactionsLoading] = useState(false);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const isFormField =
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.isContentEditable);
      if (isFormField) return;
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

  useEffect(() => {
    if (!item) return;
    let cancelled = false;
    setReactions(null);
    setReactionsLoading(true);
    getReactions(albumId, item.id)
      .then((resp) => {
        if (cancelled) return;
        if (resp?.success && resp.data) {
          setReactions(resp.data as MediaReactionsDto);
        }
      })
      .catch((err) => console.error('Failed to load reactions:', err))
      .finally(() => {
        if (!cancelled) setReactionsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [albumId, item?.id]);

  async function handleToggleReaction(type: ReactionType) {
    if (!item || reactionsLoading) return;
    setReactionsLoading(true);
    try {
      const resp = await toggleReaction(albumId, item.id, type);
      if (resp?.success && resp.data) {
        setReactions(resp.data as MediaReactionsDto);
      }
    } catch (err) {
      console.error('Failed to toggle reaction:', err);
    } finally {
      setReactionsLoading(false);
    }
  }

  if (!item) return null;

  return (
    <>
      <style>{`
        .lightbox-overlay {
          position: fixed;
          inset: 0;
          background-color: rgba(26, 26, 46, 0.95);
          z-index: 1000;
          display: flex;
          align-items: stretch;
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
          z-index: 1003;
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
          z-index: 1003;
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
          z-index: 1003;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: background 0.2s;
        }
        .lightbox-arrow:hover {
          background: rgba(253, 248, 239, 0.22);
        }
        .lightbox-arrow-left { left: 1rem; }
        .lightbox-arrow-right {
          right: calc(420px + 1rem);
        }

        .lightbox-main {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 4.5rem 1rem 1rem;
          gap: 0.9rem;
        }

        .lightbox-stage {
          flex: 1;
          min-height: 0;
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .lightbox-image {
          max-width: 100%;
          max-height: 100%;
          object-fit: contain;
        }

        .lightbox-video {
          max-width: 100%;
          max-height: 100%;
          outline: none;
        }

        .lightbox-reaction-row {
          width: min(900px, 100%);
        }

        .lightbox-sidebar {
          width: 380px;
          flex-shrink: 0;
          background: rgba(26, 26, 46, 0.6);
          border-left: 1px solid rgba(253, 248, 239, 0.08);
          padding: 4.5rem 1rem 1rem;
          overflow-y: auto;
        }

        @media (max-width: 900px) {
          .lightbox-overlay {
            flex-direction: column;
          }
          .lightbox-arrow-right {
            right: 1rem;
          }
          .lightbox-sidebar {
            width: 100%;
            border-left: none;
            border-top: 1px solid rgba(253, 248, 239, 0.08);
            padding: 0.9rem 1rem 1rem;
            max-height: 45vh;
          }
          .lightbox-main {
            padding: 4.5rem 1rem 0.5rem;
          }
        }
      `}</style>

      <div className="lightbox-overlay" onClick={onClose}>
        <button className="lightbox-close" onClick={(e) => { e.stopPropagation(); onClose(); }}>
          &#x2715;
        </button>

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

        <div className="lightbox-main" onClick={(e) => e.stopPropagation()}>
          <div className="lightbox-stage">
            {item.type === 'VIDEO' ? (
              <video
                key={item.id}
                className="lightbox-video"
                src={item.url}
                controls
                autoPlay
              />
            ) : (
              <img
                className="lightbox-image"
                src={item.url}
                alt={item.filename}
              />
            )}
          </div>

          <div className="lightbox-reaction-row">
            <ReactionBar
              data={reactions}
              loading={reactionsLoading}
              onToggle={handleToggleReaction}
            />
          </div>
        </div>

        {hasNext && (
          <button
            className="lightbox-arrow lightbox-arrow-right"
            onClick={(e) => { e.stopPropagation(); onNavigate(currentIndex + 1); }}
          >
            &#8250;
          </button>
        )}

        <aside className="lightbox-sidebar" onClick={(e) => e.stopPropagation()}>
          <CommentThread
            albumId={albumId}
            mediaId={item.id}
            currentUserId={currentUserId}
            albumOwnerId={albumOwnerId}
          />
        </aside>
      </div>
    </>
  );
}
