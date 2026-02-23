import { MediaDto } from '../api/media';

interface MediaGridProps {
  media: MediaDto[];
  onSelect: (index: number) => void;
}

export function MediaGrid({ media, onSelect }: MediaGridProps) {
  if (media.length === 0) {
    return (
      <p style={{ color: '#999', textAlign: 'center', padding: '2rem 0' }}>
        No media in this album yet.
      </p>
    );
  }

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gap: '4px',
      }}
    >
      {media.map((item, index) => (
        <div
          key={item.id}
          onClick={() => onSelect(index)}
          style={{
            position: 'relative',
            paddingBottom: '100%',
            cursor: 'pointer',
            overflow: 'hidden',
            backgroundColor: '#f0f0f0',
            borderRadius: '4px',
          }}
        >
          <img
            src={item.thumbnailUrl || item.url}
            alt={item.filename}
            loading="lazy"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              objectFit: 'cover',
            }}
          />
          {item.type === 'VIDEO' && (
            <div
              style={{
                position: 'absolute',
                bottom: '4px',
                right: '4px',
                backgroundColor: 'rgba(0,0,0,0.6)',
                color: '#fff',
                fontSize: '0.7rem',
                padding: '2px 6px',
                borderRadius: '4px',
              }}
            >
              VIDEO
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
