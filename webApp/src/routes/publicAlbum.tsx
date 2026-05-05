import { useState, useEffect } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import {
  getPublicAlbum,
  PublicAlbumDto,
  PublicMediaDto,
} from '../api/publicAlbums';

export function PublicAlbumPage() {
  const { albumId } = useParams({ from: '/public/$albumId' });
  const [album, setAlbum] = useState<PublicAlbumDto | null>(null);
  const [media, setMedia] = useState<PublicMediaDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getPublicAlbum(albumId)
      .then((resp) => {
        if (cancelled) return;
        if (resp.success && resp.data) {
          setAlbum(resp.data.album);
          setMedia(resp.data.media);
        } else {
          setError(resp.error ?? 'This album is not available.');
        }
      })
      .catch((err) => {
        if (cancelled) return;
        console.error('Failed to load public album:', err);
        setError('Failed to load album. Please try again.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [albumId]);

  // Close lightbox on Escape and arrow-key navigation
  useEffect(() => {
    if (lightboxIndex === null) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setLightboxIndex(null);
      else if (e.key === 'ArrowRight') {
        setLightboxIndex((idx) => (idx === null ? null : Math.min(media.length - 1, idx + 1)));
      } else if (e.key === 'ArrowLeft') {
        setLightboxIndex((idx) => (idx === null ? null : Math.max(0, idx - 1)));
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [lightboxIndex, media.length]);

  const ownerInitial = album?.ownerDisplayName.charAt(0).toUpperCase() ?? '?';

  return (
    <>
      <style>{`
        .pub-page {
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          min-height: 100vh;
        }

        .pub-topbar {
          position: sticky;
          top: 0;
          z-index: 30;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 var(--space-8);
          height: 60px;
          background: rgba(253, 248, 239, 0.92);
          backdrop-filter: blur(12px);
          -webkit-backdrop-filter: blur(12px);
          border-bottom: 1px solid rgba(27, 127, 204, 0.08);
        }
        .pub-logo {
          font-family: var(--font-display);
          font-size: 1.25rem;
          font-weight: 800;
          color: var(--color-deep-navy);
          text-decoration: none;
          letter-spacing: 0.02em;
        }
        .pub-signin {
          font-size: 0.78rem;
          font-weight: 600;
          color: var(--color-sky-blue);
          text-decoration: none;
          padding: 0.4rem 0.95rem;
          border: 1px solid rgba(27,127,204,0.25);
          border-radius: var(--radius-full);
          transition: all 0.18s;
        }
        .pub-signin:hover {
          background: var(--color-sky-blue);
          color: #fff;
          border-color: var(--color-sky-blue);
        }

        .pub-content {
          max-width: 1100px;
          margin: 0 auto;
          padding: var(--space-8);
        }

        .pub-hero {
          position: relative;
          aspect-ratio: 16 / 7;
          border-radius: var(--radius-lg);
          overflow: hidden;
          background: var(--color-sandy);
          margin-bottom: var(--space-6);
          box-shadow: var(--shadow-md);
        }
        .pub-hero img {
          position: absolute;
          inset: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        .pub-hero-vignette {
          position: absolute;
          inset: 0;
          background: linear-gradient(to top, rgba(26,26,46,0.7) 0%, rgba(26,26,46,0.15) 50%, transparent 80%);
        }
        .pub-hero-fallback {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: 5rem;
          font-weight: 900;
          color: var(--color-sky-blue);
          opacity: 0.45;
        }
        .pub-hero-meta {
          position: absolute;
          left: var(--space-6);
          right: var(--space-6);
          bottom: var(--space-6);
          color: #fff;
        }
        .pub-hero-title {
          font-family: var(--font-display);
          font-size: 2.4rem;
          font-weight: 800;
          margin: 0 0 0.4rem;
          line-height: 1.1;
          text-shadow: 0 2px 8px rgba(0,0,0,0.3);
        }
        .pub-hero-desc {
          font-size: 0.95rem;
          line-height: 1.5;
          opacity: 0.92;
          max-width: 640px;
          text-shadow: 0 1px 4px rgba(0,0,0,0.4);
        }

        .pub-owner-row {
          display: flex;
          align-items: center;
          gap: 0.65rem;
          margin: -0.25rem 0 var(--space-6);
          font-size: 0.85rem;
          color: var(--color-muted-slate);
        }
        .pub-owner-avatar {
          width: 28px;
          height: 28px;
          border-radius: var(--radius-full);
          object-fit: cover;
          border: 2px solid var(--color-sandy);
        }
        .pub-owner-initial {
          width: 28px;
          height: 28px;
          border-radius: var(--radius-full);
          background: var(--color-sandy);
          border: 2px solid rgba(27, 127, 204, 0.15);
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: 0.78rem;
          font-weight: 700;
          color: var(--color-sky-blue);
        }
        .pub-owner-name {
          color: var(--color-deep-navy);
          font-weight: 600;
        }
        .pub-meta-dot { opacity: 0.5; margin: 0 0.25rem; }

        .pub-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
          gap: 8px;
        }
        .pub-grid-item {
          position: relative;
          padding-bottom: 100%;
          cursor: pointer;
          overflow: hidden;
          background-color: var(--color-sandy);
          border-radius: var(--radius-sm);
          transition: transform 0.2s, box-shadow 0.2s;
        }
        .pub-grid-item:hover {
          transform: scale(1.02);
          box-shadow: var(--shadow-md);
          z-index: 1;
        }
        .pub-grid-item img,
        .pub-grid-item video {
          position: absolute;
          inset: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
        .pub-grid-play {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          background: rgba(0,0,0,0.22);
        }
        .pub-grid-play-dot {
          width: 36px;
          height: 36px;
          background: rgba(255,255,255,0.88);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .pub-empty {
          text-align: center;
          color: var(--color-muted-slate);
          padding: 4rem 0;
          font-size: 0.95rem;
        }

        .pub-loading,
        .pub-error {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 8rem 2rem;
          gap: 1rem;
          text-align: center;
        }
        .pub-spinner {
          width: 32px;
          height: 32px;
          border: 2px solid rgba(27,127,204,0.15);
          border-top-color: var(--color-sky-blue);
          border-radius: 50%;
          animation: pubSpin 0.8s linear infinite;
        }
        @keyframes pubSpin { to { transform: rotate(360deg); } }
        .pub-error-title {
          font-family: var(--font-display);
          font-size: 1.4rem;
          font-weight: 700;
          color: var(--color-deep-navy);
        }
        .pub-error-msg {
          color: var(--color-muted-slate);
          font-size: 0.9rem;
          max-width: 380px;
        }

        /* Lightbox */
        .pub-lightbox {
          position: fixed;
          inset: 0;
          background: rgba(0,0,0,0.92);
          z-index: 60;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .pub-lightbox-content {
          max-width: 92vw;
          max-height: 88vh;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .pub-lightbox-content img,
        .pub-lightbox-content video {
          max-width: 92vw;
          max-height: 88vh;
          object-fit: contain;
        }
        .pub-lightbox-close,
        .pub-lightbox-nav {
          position: absolute;
          background: rgba(255,255,255,0.12);
          border: none;
          color: #fff;
          width: 40px;
          height: 40px;
          border-radius: 50%;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 1.4rem;
        }
        .pub-lightbox-close:hover,
        .pub-lightbox-nav:hover { background: rgba(255,255,255,0.22); }
        .pub-lightbox-close { top: 18px; right: 18px; }
        .pub-lightbox-nav.prev { left: 18px; top: 50%; transform: translateY(-50%); }
        .pub-lightbox-nav.next { right: 18px; top: 50%; transform: translateY(-50%); }

        @media (max-width: 700px) {
          .pub-content { padding: var(--space-4); }
          .pub-hero { aspect-ratio: 4 / 3; }
          .pub-hero-title { font-size: 1.6rem; }
          .pub-hero-desc { font-size: 0.85rem; }
          .pub-grid { grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); }
        }
      `}</style>

      <div className="pub-page">
        <div className="pub-topbar">
          <Link to="/" className="pub-logo">Thykra</Link>
          <Link to="/login" className="pub-signin">Sign in</Link>
        </div>

        {loading ? (
          <div className="pub-loading">
            <div className="pub-spinner" />
            <div style={{ color: 'var(--color-muted-slate)', fontSize: '0.85rem' }}>Loading album...</div>
          </div>
        ) : error || !album ? (
          <div className="pub-error">
            <div className="pub-error-title">Album not available</div>
            <div className="pub-error-msg">
              {error ?? 'This album is private or no longer shared.'}
            </div>
            <Link to="/login" style={{ color: 'var(--color-sky-blue)', fontWeight: 600, fontSize: '0.85rem' }}>
              Sign in to Thykra
            </Link>
          </div>
        ) : (
          <div className="pub-content">
            <div className="pub-hero">
              {album.coverUrl ? (
                <>
                  <img src={album.coverUrl} alt={album.title} />
                  <div className="pub-hero-vignette" />
                </>
              ) : (
                <div className="pub-hero-fallback">{album.title.charAt(0).toUpperCase()}</div>
              )}
              <div className="pub-hero-meta">
                <h1 className="pub-hero-title">{album.title}</h1>
                {album.description && <p className="pub-hero-desc">{album.description}</p>}
              </div>
            </div>

            <div className="pub-owner-row">
              {album.ownerAvatarUrl ? (
                <img className="pub-owner-avatar" src={album.ownerAvatarUrl} alt="" />
              ) : (
                <div className="pub-owner-initial">{ownerInitial}</div>
              )}
              <span>
                Shared by <span className="pub-owner-name">{album.ownerDisplayName}</span>
                <span className="pub-meta-dot">&middot;</span>
                {album.mediaCount} {album.mediaCount === 1 ? 'photo' : 'photos'}
              </span>
            </div>

            {media.length === 0 ? (
              <div className="pub-empty">No photos in this album yet.</div>
            ) : (
              <div className="pub-grid">
                {media.map((item, index) => (
                  <div
                    key={item.id}
                    className="pub-grid-item"
                    onClick={() => setLightboxIndex(index)}
                  >
                    {item.type === 'VIDEO' && !item.thumbnailUrl ? (
                      <video src={item.url} muted preload="metadata" />
                    ) : (
                      <img src={item.thumbnailUrl || item.url} alt="" loading="lazy" />
                    )}
                    {item.type === 'VIDEO' && (
                      <div className="pub-grid-play">
                        <div className="pub-grid-play-dot">
                          <svg width="18" height="18" viewBox="0 0 24 24" fill="var(--color-deep-navy)">
                            <path d="M8 5v14l11-7z" />
                          </svg>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {lightboxIndex !== null && media[lightboxIndex] && (
          <div className="pub-lightbox" onClick={() => setLightboxIndex(null)}>
            <button
              className="pub-lightbox-close"
              onClick={(e) => {
                e.stopPropagation();
                setLightboxIndex(null);
              }}
              aria-label="Close"
            >
              &times;
            </button>
            {lightboxIndex > 0 && (
              <button
                className="pub-lightbox-nav prev"
                onClick={(e) => {
                  e.stopPropagation();
                  setLightboxIndex(lightboxIndex - 1);
                }}
                aria-label="Previous"
              >
                &lsaquo;
              </button>
            )}
            {lightboxIndex < media.length - 1 && (
              <button
                className="pub-lightbox-nav next"
                onClick={(e) => {
                  e.stopPropagation();
                  setLightboxIndex(lightboxIndex + 1);
                }}
                aria-label="Next"
              >
                &rsaquo;
              </button>
            )}
            <div className="pub-lightbox-content" onClick={(e) => e.stopPropagation()}>
              {media[lightboxIndex].type === 'VIDEO' ? (
                <video src={media[lightboxIndex].url} controls autoPlay />
              ) : (
                <img src={media[lightboxIndex].url} alt="" />
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
}
