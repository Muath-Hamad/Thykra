import { useState, useEffect } from 'react';
import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { getAlbums, createAlbum, AlbumDto, AlbumMemberSummary } from '../api/albums';
import { AppNav } from '../components/AppNav';

export function AlbumsPage() {
  const auth = useAuth();
  const [albums, setAlbums] = useState<AlbumDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    loadAlbums();
  }, []);

  async function loadAlbums() {
    setLoading(true);
    try {
      const data = await getAlbums();
      if (data.success && data.data) setAlbums(data.data);
    } catch (error) {
      console.error('Failed to load albums:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate() {
    setCreating(true);
    try {
      const data = await createAlbum(title, description || undefined);
      if (data.success && data.data) {
        setAlbums([...albums, data.data]);
        setShowCreate(false);
        setTitle('');
        setDescription('');
      }
    } catch (error) {
      console.error('Failed to create album:', error);
    } finally {
      setCreating(false);
    }
  }

  useEffect(() => {
    if (!showCreate) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setShowCreate(false);
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [showCreate]);

  return (
    <>
      <style>{`
        .albums-page {
          background: var(--color-warm-white);
          min-height: 100vh;
          font-family: var(--font-body);
        }

        .albums-content {
          max-width: 1280px;
          margin: 0 auto;
          padding: 100px var(--space-8) var(--space-12);
        }

        .albums-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 2rem;
        }

        .albums-title {
          font-family: var(--font-display);
          font-size: 1.8rem;
          font-weight: 800;
          margin: 0;
          color: var(--color-deep-navy);
        }

        .albums-create-btn {
          display: inline-flex;
          align-items: center;
          gap: 0.5rem;
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.7rem 1.5rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-md);
          transition: background 0.2s, transform 0.2s, box-shadow 0.2s;
          box-shadow: var(--shadow-sm);
        }
        .albums-create-btn:hover {
          background: var(--color-ocean-blue);
          transform: translateY(-1px);
          box-shadow: var(--shadow-md);
        }

        /* ── Grid ── */
        .albums-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
          gap: 1.25rem;
        }

        /* ── Card ── */
        .album-card {
          position: relative;
          display: block;
          text-decoration: none;
          border-radius: var(--radius-lg);
          overflow: hidden;
          aspect-ratio: 3 / 4;
          background: linear-gradient(145deg, var(--color-deep-navy) 0%, #1B4F8A 100%);
          box-shadow: var(--shadow-sm);
          transition: transform 0.3s ease, box-shadow 0.3s ease;
          cursor: pointer;
        }
        .album-card:hover {
          transform: translateY(-4px) scale(1.01);
          box-shadow: var(--shadow-lg);
        }

        .album-card-img {
          position: absolute;
          inset: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s ease;
        }
        .album-card:hover .album-card-img {
          transform: scale(1.04);
        }

        /* Permanent subtle vignette at the bottom so image always looks polished */
        .album-card-vignette {
          position: absolute;
          inset: 0;
          background: linear-gradient(
            to top,
            rgba(26, 26, 46, 0.55) 0%,
            transparent 45%
          );
          pointer-events: none;
        }

        /* Hover overlay — slides up from bottom */
        .album-card-overlay {
          position: absolute;
          inset: 0;
          background: linear-gradient(
            to top,
            rgba(26, 26, 46, 0.92) 0%,
            rgba(26, 26, 46, 0.55) 50%,
            rgba(26, 26, 46, 0.1) 100%
          );
          opacity: 0;
          transition: opacity 0.35s ease;
          pointer-events: none;
        }
        .album-card:hover .album-card-overlay {
          opacity: 1;
        }

        /* Info block — slides up on hover */
        .album-card-info {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          padding: 1.1rem 1rem 1rem;
          transform: translateY(6px);
          opacity: 0;
          transition: transform 0.35s ease, opacity 0.35s ease;
        }
        .album-card:hover .album-card-info {
          transform: translateY(0);
          opacity: 1;
        }

        .album-card-title {
          margin: 0 0 0.55rem;
          font-family: var(--font-display);
          font-size: 1rem;
          font-weight: 700;
          color: #fff;
          line-height: 1.3;
          text-shadow: 0 1px 4px rgba(0,0,0,0.4);
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .album-card-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        /* ── Avatar stack ── */
        .album-avatars {
          display: flex;
          align-items: center;
        }

        .album-avatar {
          width: 26px;
          height: 26px;
          border-radius: 50%;
          border: 2px solid rgba(255, 255, 255, 0.85);
          margin-right: -7px;
          flex-shrink: 0;
          object-fit: cover;
          background: var(--color-sky-blue);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 0.6rem;
          font-weight: 700;
          color: #fff;
          text-transform: uppercase;
          overflow: hidden;
        }
        .album-avatar:last-child { margin-right: 0; }

        .album-avatar-more {
          background: rgba(255, 255, 255, 0.2);
          backdrop-filter: blur(4px);
          font-size: 0.55rem;
          font-weight: 700;
          color: #fff;
          letter-spacing: 0;
        }

        .album-card-member-count {
          font-size: 0.7rem;
          color: rgba(255, 255, 255, 0.7);
          font-weight: 500;
        }

        /* ── No-image placeholder ── */
        .album-card-placeholder {
          position: absolute;
          inset: 0;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .album-card-placeholder-initial {
          font-family: var(--font-display);
          font-size: 3.5rem;
          font-weight: 800;
          color: rgba(255, 255, 255, 0.18);
          user-select: none;
        }

        /* On no-image cards always show info (no image to cover) */
        .album-card.no-image .album-card-info {
          opacity: 1;
          transform: translateY(0);
        }
        .album-card.no-image .album-card-overlay {
          opacity: 1;
        }

        /* ── Loading / Empty ── */
        .albums-loading {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 6rem 2rem;
          gap: 1rem;
        }
        .albums-spinner {
          width: 32px;
          height: 32px;
          border: 2px solid rgba(27, 127, 204, 0.15);
          border-top-color: var(--color-sky-blue);
          border-radius: 50%;
          animation: albumsSpin 0.8s linear infinite;
        }
        .albums-loading-text {
          font-size: 0.85rem;
          color: var(--color-muted-slate);
          font-weight: 500;
        }
        .albums-empty {
          text-align: center;
          padding: 5rem 2rem;
          color: var(--color-muted-slate);
          font-size: 0.95rem;
        }

        /* ── Modal ── */
        .albums-modal-overlay {
          position: fixed;
          inset: 0;
          background: rgba(26, 26, 46, 0.45);
          backdrop-filter: blur(4px);
          z-index: 100;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .albums-modal {
          background: var(--color-warm-white);
          border: 1px solid rgba(27, 127, 204, 0.1);
          border-radius: var(--radius-xl);
          padding: 2.5rem;
          width: 90%;
          max-width: 480px;
          box-shadow: var(--shadow-lg);
        }
        .albums-modal-title {
          font-family: var(--font-display);
          font-size: 1.4rem;
          font-weight: 800;
          margin: 0 0 1.5rem;
          color: var(--color-deep-navy);
        }
        .albums-modal-field { margin-bottom: 1.2rem; }
        .albums-modal-label {
          display: block;
          font-size: 0.75rem;
          font-weight: 500;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          color: var(--color-muted-slate);
          margin-bottom: 0.4rem;
        }
        .albums-modal-input {
          width: 100%;
          padding: 0.7rem 1rem;
          background: var(--color-sandy);
          border: 1px solid rgba(27, 127, 204, 0.1);
          border-radius: var(--radius-sm);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          font-size: 0.9rem;
          outline: none;
          transition: border-color 0.2s;
          box-sizing: border-box;
        }
        .albums-modal-input:focus { border-color: var(--color-sky-blue); }
        .albums-modal-input::placeholder { color: var(--color-muted-slate); opacity: 0.6; }
        .albums-modal-actions {
          display: flex;
          gap: 0.75rem;
          justify-content: flex-end;
          margin-top: 1.5rem;
        }
        .albums-modal-cancel {
          background: transparent;
          border: 1px solid rgba(27, 127, 204, 0.15);
          color: var(--color-muted-slate);
          padding: 0.6rem 1.2rem;
          font-size: 0.8rem;
          font-weight: 500;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: all 0.2s;
        }
        .albums-modal-cancel:hover { color: var(--color-deep-navy); border-color: rgba(27,127,204,0.3); }
        .albums-modal-submit {
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.6rem 1.6rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-sm);
          transition: background 0.2s;
        }
        .albums-modal-submit:hover { background: var(--color-ocean-blue); }
        .albums-modal-submit:disabled { opacity: 0.4; cursor: not-allowed; }

        @keyframes albumsSpin { to { transform: rotate(360deg); } }

        @media (max-width: 900px) {
          .albums-grid { grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; }
        }
        @media (max-width: 600px) {
          .albums-content { padding: 90px var(--space-4) var(--space-6); }
          .albums-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
          .albums-grid { grid-template-columns: repeat(2, 1fr); gap: 0.75rem; }
          /* Always show info on touch screens */
          .album-card-info { opacity: 1; transform: translateY(0); }
          .album-card-overlay { opacity: 1; }
        }
      `}</style>

      <div className="albums-page">
        <AppNav />

        <div className="albums-content">
          <div className="albums-header">
            <h1 className="albums-title">All Trips</h1>
            <button className="albums-create-btn" onClick={() => setShowCreate(true)}>
              <svg width="13" height="13" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M8 3v10M3 8h10" />
              </svg>
              New Trip
            </button>
          </div>

          {loading ? (
            <div className="albums-loading">
              <div className="albums-spinner" />
              <div className="albums-loading-text">Loading trips...</div>
            </div>
          ) : albums.length === 0 ? (
            <div className="albums-empty">
              No trips yet. Create one to get started.
            </div>
          ) : (
            <div className="albums-grid">
              {albums.map((album) => (
                <AlbumCard key={album.id} album={album} currentUserId={auth.user?.id} />
              ))}
            </div>
          )}
        </div>

        {showCreate && (
          <div className="albums-modal-overlay" onClick={(e) => {
            if (e.target === e.currentTarget) setShowCreate(false);
          }}>
            <div className="albums-modal">
              <h2 className="albums-modal-title">Create New Trip</h2>
              <div className="albums-modal-field">
                <label className="albums-modal-label">Trip Title</label>
                <input
                  className="albums-modal-input"
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Summer in Italy 2024"
                  autoFocus
                  onKeyDown={(e) => { if (e.key === 'Enter' && title.trim()) handleCreate(); }}
                />
              </div>
              <div className="albums-modal-field">
                <label className="albums-modal-label">Description (optional)</label>
                <input
                  className="albums-modal-input"
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="A brief description..."
                />
              </div>
              <div className="albums-modal-actions">
                <button className="albums-modal-cancel" onClick={() => { setShowCreate(false); setTitle(''); setDescription(''); }}>
                  Cancel
                </button>
                <button className="albums-modal-submit" onClick={handleCreate} disabled={creating || !title.trim()}>
                  {creating ? 'Creating...' : 'Create Trip'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

function AlbumCard({ album }: { album: AlbumDto; currentUserId?: string }) {
  const hasImage = !!album.coverUrl;

  return (
    <Link
      to="/albums/$albumId"
      params={{ albumId: album.id }}
      className={`album-card${hasImage ? '' : ' no-image'}`}
    >
      {hasImage ? (
        <img className="album-card-img" src={album.coverUrl} alt={album.title} loading="lazy" />
      ) : (
        <div className="album-card-placeholder">
          <span className="album-card-placeholder-initial">
            {album.title.charAt(0).toUpperCase()}
          </span>
        </div>
      )}

      <div className="album-card-vignette" />
      <div className="album-card-overlay" />

      <div className="album-card-info">
        <h3 className="album-card-title">{album.title}</h3>
        <div className="album-card-footer">
          <AvatarStack members={album.previewMembers} totalCount={album.memberCount} />
          <span className="album-card-member-count">
            {album.memberCount} {album.memberCount === 1 ? 'member' : 'members'}
          </span>
        </div>
      </div>
    </Link>
  );
}

function AvatarStack({ members, totalCount }: { members: AlbumMemberSummary[]; totalCount: number }) {
  const shown = members.slice(0, 4);
  const extra = totalCount - shown.length;

  return (
    <div className="album-avatars">
      {shown.map((member) =>
        member.avatarUrl ? (
          <img
            key={member.userId}
            className="album-avatar"
            src={member.avatarUrl}
            alt={member.displayName}
            title={member.displayName}
          />
        ) : (
          <div
            key={member.userId}
            className="album-avatar"
            title={member.displayName}
          >
            {member.displayName.charAt(0).toUpperCase()}
          </div>
        )
      )}
      {extra > 0 && (
        <div className="album-avatar album-avatar-more">+{extra}</div>
      )}
    </div>
  );
}
