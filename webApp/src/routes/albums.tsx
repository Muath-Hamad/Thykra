import { useState, useEffect } from 'react';
import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { getAlbums, createAlbum, AlbumDto } from '../api/albums';
import { AppNav } from '../components/AppNav';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

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
      if (data.success && data.data) {
        setAlbums(data.data);
      }
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
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          min-height: 100vh;
        }

        .albums-content {
          max-width: 1200px;
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
          gap: 0.6rem;
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.7rem 1.6rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-md);
          transition: all 0.3s;
          box-shadow: var(--shadow-sm);
        }
        .albums-create-btn:hover {
          background: var(--color-ocean-blue);
          box-shadow: var(--shadow-md);
          transform: translateY(-1px);
        }

        /* Grid */
        .albums-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
          gap: 1.5rem;
        }

        .albums-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.06);
          border-radius: var(--radius-lg);
          padding: 1.5rem;
          text-decoration: none;
          color: inherit;
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
          box-shadow: var(--shadow-sm);
          transition: box-shadow 0.3s, transform 0.3s;
        }
        .albums-card:hover {
          box-shadow: var(--shadow-md);
          transform: translateY(-3px);
        }

        .albums-card-title {
          font-family: var(--font-display);
          font-size: 1.1rem;
          font-weight: 700;
          color: var(--color-deep-navy);
          margin: 0;
        }

        .albums-card-desc {
          font-size: 0.85rem;
          color: var(--color-muted-slate);
          margin: 0;
          line-height: 1.5;
        }

        .albums-card-meta {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding-top: 0.75rem;
          border-top: 1px solid rgba(27,127,204,0.06);
          margin-top: 0.25rem;
        }

        .albums-card-badge {
          font-size: 0.65rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          padding: 0.2rem 0.6rem;
          border-radius: var(--radius-full);
        }
        .albums-badge-owner {
          background: rgba(27,127,204,0.1);
          color: var(--color-sky-blue);
        }
        .albums-badge-member {
          background: var(--color-sandy);
          color: var(--color-muted-slate);
          border: 1px solid rgba(27,127,204,0.1);
        }

        .albums-card-stat {
          font-size: 0.72rem;
          color: var(--color-muted-slate);
        }

        /* Loading / Empty */
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
          border: 2px solid rgba(27,127,204,0.15);
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

        /* Modal */
        .albums-modal-overlay {
          position: fixed;
          inset: 0;
          background: rgba(26,26,46,0.4);
          backdrop-filter: blur(4px);
          z-index: 100;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .albums-modal {
          background: var(--color-warm-white);
          border: 1px solid rgba(27,127,204,0.1);
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

        .albums-modal-field {
          margin-bottom: 1.2rem;
        }

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
          border: 1px solid rgba(27,127,204,0.1);
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
          border: 1px solid rgba(27,127,204,0.15);
          color: var(--color-muted-slate);
          padding: 0.6rem 1.2rem;
          font-size: 0.8rem;
          font-weight: 500;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: all 0.2s;
        }
        .albums-modal-cancel:hover {
          color: var(--color-deep-navy);
          border-color: rgba(27,127,204,0.3);
        }

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

        @keyframes albumsSpin {
          to { transform: rotate(360deg); }
        }

        @media (max-width: 768px) {
          .albums-content { padding: 90px var(--space-4) var(--space-6); }
          .albums-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
          .albums-grid { grid-template-columns: 1fr; }
        }
      `}</style>

      <div className="albums-page">
        <AppNav />

        <div className="albums-content">
          <div className="albums-header">
            <h1 className="albums-title">All Trips</h1>
            <button className="albums-create-btn" onClick={() => setShowCreate(true)}>
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
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
              {albums.map((album) => {
                const isOwner = album.ownerId === auth.user?.id;
                return (
                  <Link
                    key={album.id}
                    to="/albums/$albumId"
                    params={{ albumId: album.id }}
                    className="albums-card"
                  >
                    <h3 className="albums-card-title">{album.title}</h3>
                    {album.description && (
                      <p className="albums-card-desc">{album.description}</p>
                    )}
                    <div className="albums-card-meta">
                      <span className={`albums-card-badge ${isOwner ? 'albums-badge-owner' : 'albums-badge-member'}`}>
                        {isOwner ? 'Owner' : 'Member'}
                      </span>
                      <span className="albums-card-stat">
                        {album.memberCount} {album.memberCount === 1 ? 'member' : 'members'}
                      </span>
                      <span className="albums-card-stat">
                        {formatDate(album.createdAt)}
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </div>

        {/* Create modal */}
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
