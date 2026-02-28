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

        .media-grid-video-badge {
          position: absolute;
          bottom: 6px;
          right: 6px;
          background-color: var(--color-deep-navy);
          color: #fff;
          font-size: 0.65rem;
          font-weight: 600;
          font-family: var(--font-body);
          padding: 3px 8px;
          border-radius: var(--radius-full);
          letter-spacing: 0.03em;
        }
      `}</style>

      <div className="media-grid">
        {media.map((item, index) => (
          <div
            key={item.id}
            className="media-grid-item"
            onClick={() => onSelect(index)}
          >
            <img
              src={item.thumbnailUrl || item.url}
              alt={item.filename}
              loading="lazy"
            />
            {item.type === 'VIDEO' && (
              <div className="media-grid-video-badge">VIDEO</div>
            )}
          </div>
        ))}
      </div>
    </>
  );
}
