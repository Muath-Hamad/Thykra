import { MediaDto } from '../api/media';

interface MediaGridProps {
  media: MediaDto[];
  onSelect: (index: number) => void;
}

export function MediaGrid({ media, onSelect }: MediaGridProps) {
  if (media.length === 0) {
    return (
      <p style={{
        color: 'var(--color-muted-slate)',
        textAlign: 'center',
        padding: '3rem 0',
        fontFamily: 'var(--font-body)',
        fontSize: '0.9rem',
      }}>
        No media in this album yet. Upload some photos to get started.
      </p>
    );
  }

  return (
    <>
      <style>{`
        .media-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 8px;
        }

        .media-grid-item {
          position: relative;
          padding-bottom: 100%;
          cursor: pointer;
          overflow: hidden;
          background-color: var(--color-sandy);
          border-radius: var(--radius-sm);
          transition: transform 0.2s, box-shadow 0.2s;
        }
        .media-grid-item:hover {
          transform: scale(1.02);
          box-shadow: var(--shadow-md);
          z-index: 1;
        }

        .media-grid-item img {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .media-grid-video-overlay {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          background: rgba(0, 0, 0, 0.22);
        }

        .media-grid-play-icon {
          width: 36px;
          height: 36px;
          background: rgba(255, 255, 255, 0.88);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .media-grid-play-icon svg {
          width: 18px;
          height: 18px;
          margin-left: 3px;
        }

        .media-grid-no-thumb {
          position: absolute;
          inset: 0;
          background: var(--color-deep-navy);
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .media-grid-no-thumb svg {
          width: 32px;
          height: 32px;
          opacity: 0.6;
        }
      `}</style>

      <div className="media-grid">
        {media.map((item, index) => (
          <div
            key={item.id}
            className="media-grid-item"
            onClick={() => onSelect(index)}
          >
            {item.type === 'VIDEO' && !item.thumbnailUrl ? (
              <div className="media-grid-no-thumb">
                <svg viewBox="0 0 24 24" fill="white"><path d="M8 5v14l11-7z"/></svg>
              </div>
            ) : (
              <img
                src={item.thumbnailUrl || item.url}
                alt={item.filename}
                loading="lazy"
              />
            )}
            {item.type === 'VIDEO' && (
              <div className="media-grid-video-overlay">
                <div className="media-grid-play-icon">
                  <svg viewBox="0 0 24 24" fill="var(--color-deep-navy)"><path d="M8 5v14l11-7z"/></svg>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </>
  );
}
