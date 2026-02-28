import { useState, useEffect } from 'react';
import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { getAlbums, createAlbum, type AlbumDto } from '../api/albums';
import { getAlbumMedia } from '../api/media';
import { AppNav } from '../components/AppNav';

function getGreeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}

function getFirstName(displayName: string): string {
  return displayName.split(' ')[0];
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function HomePage() {
  const auth = useAuth();
  const [albums, setAlbums] = useState<AlbumDto[]>([]);
  const [mediaCounts, setMediaCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await getAlbums();
        if (cancelled) return;
        if (res.success && res.data) {
          const list: AlbumDto[] = res.data;
          setAlbums(list);

          // Fetch media counts in parallel
          const counts: Record<string, number> = {};
          await Promise.all(
            list.map(async (a) => {
              try {
                const mRes = await getAlbumMedia(a.id);
                counts[a.id] = mRes.success && Array.isArray(mRes.data) ? mRes.data.length : 0;
              } catch {
                counts[a.id] = 0;
              }
            })
          );
          if (!cancelled) setMediaCounts(counts);
        }
      } catch {
        // silent
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const totalPhotos = Object.values(mediaCounts).reduce((s, c) => s + c, 0);
  const totalCollaborators = albums.reduce((s, a) => s + a.memberCount, 0);

  const handleCreate = async () => {
    if (!newTitle.trim() || creating) return;
    setCreating(true);
    try {
      const res = await createAlbum(newTitle.trim(), newDesc.trim() || undefined);
      if (res.success && res.data) {
        setAlbums(prev => [...prev, res.data]);
        setMediaCounts(prev => ({ ...prev, [res.data.id]: 0 }));
        setShowModal(false);
        setNewTitle('');
        setNewDesc('');
      }
    } catch {
      // silent
    } finally {
      setCreating(false);
    }
  };

  useEffect(() => {
    if (!showModal) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setShowModal(false);
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [showModal]);

  const userName = auth.user?.displayName || 'there';

  return (
    <>
      <style>{`
        .home-page {
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          min-height: 100vh;
        }

        /* ─── MAIN CONTENT ─── */
        .home-content {
          max-width: 1200px;
          margin: 0 auto;
          padding: 100px var(--space-8) var(--space-12);
        }

        /* ─── GREETING ─── */
        .home-greeting-section {
          margin-bottom: 3rem;
        }

        .home-label {
          font-size: 0.75rem;
          letter-spacing: 0.15em;
          text-transform: uppercase;
          color: var(--color-sky-blue);
          font-weight: 600;
          margin-bottom: 0.8rem;
          animation: homeFadeUp 0.8s ease both;
        }

        .home-greeting {
          font-family: var(--font-display);
          font-size: clamp(2.2rem, 4vw, 3.5rem);
          font-weight: 900;
          line-height: 1.15;
          margin: 0 0 0.6rem;
          color: var(--color-deep-navy);
          animation: homeFadeUp 0.8s ease 0.1s both;
        }

        .home-greeting em {
          font-style: italic;
          font-weight: 400;
          color: var(--color-sky-blue);
        }

        .home-subtitle {
          font-size: 0.95rem;
          color: var(--color-muted-slate);
          font-weight: 400;
          animation: homeFadeUp 0.8s ease 0.2s both;
        }

        /* ─── STATS ─── */
        .home-stats {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 1.5rem;
          margin-bottom: 3.5rem;
        }

        .home-stat-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.06);
          border-radius: var(--radius-md);
          padding: 1.8rem 1.5rem;
          box-shadow: var(--shadow-sm);
          transition: box-shadow 0.3s, transform 0.3s;
        }
        .home-stat-card:hover {
          box-shadow: var(--shadow-md);
          transform: translateY(-2px);
        }

        .home-stat-card:nth-child(1) { animation: homeFadeUp 0.8s ease 0.25s both; }
        .home-stat-card:nth-child(2) { animation: homeFadeUp 0.8s ease 0.35s both; }
        .home-stat-card:nth-child(3) { animation: homeFadeUp 0.8s ease 0.45s both; }

        .home-stat-number {
          font-family: var(--font-display);
          font-size: 2.5rem;
          font-weight: 800;
          color: var(--color-deep-navy);
          line-height: 1;
          margin-bottom: 0.5rem;
        }

        .home-stat-label {
          font-size: 0.75rem;
          font-weight: 500;
          letter-spacing: 0.1em;
          text-transform: uppercase;
          color: var(--color-muted-slate);
        }

        /* ─── ALBUMS SECTION ─── */
        .home-section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 2rem;
          animation: homeFadeUp 0.8s ease 0.5s both;
        }

        .home-section-title {
          font-family: var(--font-display);
          font-size: 1.6rem;
          font-weight: 800;
          margin: 0;
          color: var(--color-deep-navy);
        }

        .home-cta-btn {
          display: inline-flex;
          align-items: center;
          gap: 0.6rem;
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.7rem 1.6rem;
          font-size: 0.8rem;
          font-weight: 600;
          letter-spacing: 0.02em;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-md);
          transition: all 0.3s;
          box-shadow: var(--shadow-sm);
        }
        .home-cta-btn:hover {
          background: var(--color-ocean-blue);
          box-shadow: var(--shadow-md);
          transform: translateY(-1px);
        }

        /* ─── ALBUM GRID ─── */
        .home-album-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
          gap: 1.5rem;
        }

        .home-album-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.06);
          border-radius: var(--radius-lg);
          overflow: hidden;
          cursor: pointer;
          transition: box-shadow 0.3s, transform 0.3s;
          text-decoration: none;
          color: inherit;
          display: flex;
          flex-direction: column;
          box-shadow: var(--shadow-sm);
        }
        .home-album-card:hover {
          box-shadow: var(--shadow-md);
          transform: translateY(-3px);
        }

        .home-album-cover {
          width: 100%;
          aspect-ratio: 16 / 9;
          overflow: hidden;
          position: relative;
          background: var(--color-sandy);
        }

        .home-album-cover img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s;
        }
        .home-album-card:hover .home-album-cover img {
          transform: scale(1.05);
        }

        .home-album-cover::after {
          content: '';
          position: absolute;
          inset: 0;
          background: linear-gradient(180deg, transparent 50%, rgba(26,26,46,0.2) 100%);
          pointer-events: none;
        }

        .home-album-photo-count {
          position: absolute;
          top: 0.75rem;
          right: 0.75rem;
          background: rgba(26,26,46,0.65);
          color: #fff;
          font-size: 0.7rem;
          font-weight: 600;
          padding: 0.25rem 0.6rem;
          border-radius: var(--radius-full);
          z-index: 2;
          display: flex;
          align-items: center;
          gap: 0.3rem;
        }

        .home-album-placeholder {
          width: 100%;
          aspect-ratio: 16 / 9;
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: 3rem;
          font-weight: 800;
          color: rgba(27,127,204,0.2);
          background: linear-gradient(135deg, var(--color-sandy), rgba(245,230,200,0.5));
        }

        .home-album-body {
          padding: 1.5rem;
          flex: 1;
          display: flex;
          flex-direction: column;
        }

        .home-album-title {
          font-family: var(--font-display);
          font-size: 1.15rem;
          font-weight: 700;
          margin: 0 0 0.4rem;
          color: var(--color-deep-navy);
        }

        .home-album-desc {
          font-size: 0.85rem;
          color: var(--color-muted-slate);
          font-weight: 400;
          line-height: 1.6;
          margin: 0 0 1rem;
          overflow: hidden;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          flex: 1;
        }

        .home-album-meta {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding-top: 1rem;
          border-top: 1px solid rgba(27,127,204,0.06);
        }

        .home-album-badge {
          font-size: 0.65rem;
          letter-spacing: 0.06em;
          text-transform: uppercase;
          padding: 0.25rem 0.6rem;
          border-radius: var(--radius-full);
          font-weight: 600;
        }
        .home-badge-owner {
          background: rgba(27,127,204,0.1);
          color: var(--color-sky-blue);
        }
        .home-badge-member {
          background: var(--color-sandy);
          color: var(--color-muted-slate);
          border: 1px solid rgba(27,127,204,0.1);
        }

        .home-album-meta-text {
          font-size: 0.72rem;
          color: var(--color-muted-slate);
        }

        /* ─── NEW ALBUM CARD ─── */
        .home-new-album-card {
          border: 2px dashed rgba(27,127,204,0.3);
          border-radius: var(--radius-lg);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 1rem;
          padding: 3rem 2rem;
          min-height: 320px;
          cursor: pointer;
          transition: all 0.3s;
          background: transparent;
          color: inherit;
          font-family: inherit;
        }
        .home-new-album-card:hover {
          border-color: var(--color-sky-blue);
          background: rgba(27,127,204,0.04);
        }

        .home-new-album-icon {
          width: 48px;
          height: 48px;
          border-radius: var(--radius-full);
          border: 2px solid rgba(27,127,204,0.3);
          display: flex;
          align-items: center;
          justify-content: center;
          color: var(--color-sky-blue);
          transition: all 0.3s;
        }
        .home-new-album-card:hover .home-new-album-icon {
          background: rgba(27,127,204,0.1);
          border-color: var(--color-sky-blue);
        }

        .home-new-album-label {
          font-size: 0.85rem;
          font-weight: 500;
          color: var(--color-muted-slate);
          transition: color 0.3s;
        }
        .home-new-album-card:hover .home-new-album-label { color: var(--color-sky-blue); }

        /* ─── EMPTY STATE ─── */
        .home-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 5rem 2rem;
          text-align: center;
          animation: homeFadeUp 0.8s ease 0.55s both;
        }

        .home-empty-icon {
          color: var(--color-sky-blue);
          opacity: 0.6;
          margin-bottom: 2rem;
        }

        .home-empty-heading {
          font-family: var(--font-display);
          font-size: 1.8rem;
          font-weight: 800;
          margin: 0 0 0.8rem;
          color: var(--color-deep-navy);
        }

        .home-empty-desc {
          font-size: 0.95rem;
          color: var(--color-muted-slate);
          font-weight: 400;
          line-height: 1.7;
          max-width: 400px;
          margin: 0 0 2.5rem;
        }

        /* ─── LOADING ─── */
        .home-loading {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 8rem 2rem;
          gap: 1.5rem;
          animation: homeFadeIn 0.5s ease both;
        }

        .home-spinner {
          width: 32px;
          height: 32px;
          border: 2px solid rgba(27,127,204,0.15);
          border-top-color: var(--color-sky-blue);
          border-radius: 50%;
          animation: homeSpin 0.8s linear infinite;
        }

        .home-loading-text {
          font-size: 0.8rem;
          color: var(--color-muted-slate);
          letter-spacing: 0.08em;
          text-transform: uppercase;
          font-weight: 500;
        }

        /* ─── MODAL ─── */
        .home-modal-overlay {
          position: fixed;
          inset: 0;
          background: rgba(26,26,46,0.4);
          backdrop-filter: blur(4px);
          z-index: 100;
          display: flex;
          align-items: center;
          justify-content: center;
          animation: homeFadeIn 0.3s ease both;
        }

        .home-modal-card {
          background: var(--color-warm-white);
          border: 1px solid rgba(27,127,204,0.1);
          border-radius: var(--radius-xl);
          padding: 2.5rem;
          width: 90%;
          max-width: 480px;
          position: relative;
          animation: homeFadeUp 0.4s ease both;
          box-shadow: var(--shadow-lg);
        }

        .home-modal-title {
          font-family: var(--font-display);
          font-size: 1.5rem;
          font-weight: 800;
          margin: 0 0 2rem;
          color: var(--color-deep-navy);
        }

        .home-modal-field {
          margin-bottom: 1.5rem;
        }

        .home-modal-label {
          display: block;
          font-size: 0.75rem;
          font-weight: 500;
          letter-spacing: 0.06em;
          text-transform: uppercase;
          color: var(--color-muted-slate);
          margin-bottom: 0.5rem;
        }

        .home-modal-input {
          width: 100%;
          padding: 0.8rem 1rem;
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.1);
          border-radius: var(--radius-sm);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          font-size: 0.9rem;
          outline: none;
          transition: border-color 0.3s;
          box-sizing: border-box;
        }
        .home-modal-input:focus {
          border-color: var(--color-sky-blue);
        }
        .home-modal-input::placeholder {
          color: var(--color-muted-slate);
          opacity: 0.6;
        }

        .home-modal-textarea {
          width: 100%;
          padding: 0.8rem 1rem;
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.1);
          border-radius: var(--radius-sm);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          font-size: 0.9rem;
          outline: none;
          transition: border-color 0.3s;
          resize: vertical;
          min-height: 80px;
          box-sizing: border-box;
        }
        .home-modal-textarea:focus {
          border-color: var(--color-sky-blue);
        }
        .home-modal-textarea::placeholder {
          color: var(--color-muted-slate);
          opacity: 0.6;
        }

        .home-modal-actions {
          display: flex;
          gap: 1rem;
          justify-content: flex-end;
          margin-top: 2rem;
        }

        .home-modal-cancel {
          background: none;
          border: 1px solid rgba(27,127,204,0.15);
          color: var(--color-muted-slate);
          padding: 0.7rem 1.5rem;
          font-size: 0.8rem;
          font-weight: 500;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: all 0.3s;
        }
        .home-modal-cancel:hover {
          color: var(--color-deep-navy);
          border-color: rgba(27,127,204,0.3);
        }

        .home-modal-submit {
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.7rem 1.8rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-sm);
          transition: all 0.3s;
        }
        .home-modal-submit:hover {
          background: var(--color-ocean-blue);
        }
        .home-modal-submit:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }

        /* ─── ANIMATIONS ─── */
        @keyframes homeFadeUp {
          from { opacity: 0; transform: translateY(25px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes homeFadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }

        @keyframes homeSpin {
          to { transform: rotate(360deg); }
        }

        /* ─── RESPONSIVE ─── */
        @media (max-width: 1280px) {
          .home-content { padding: 100px var(--space-6) var(--space-8); }
        }

        @media (max-width: 768px) {
          .home-content { padding: 90px var(--space-4) var(--space-6); }
          .home-stats { grid-template-columns: repeat(3, 1fr); gap: 1rem; }
          .home-stat-card { padding: 1.2rem 1rem; }
          .home-stat-number { font-size: 2rem; }
          .home-album-grid { grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }
        }

        @media (max-width: 375px) {
          .home-content { padding: 85px var(--space-3) var(--space-4); }
          .home-greeting { font-size: 1.8rem; }
          .home-stats { grid-template-columns: 1fr; }
          .home-section-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
          .home-album-grid { grid-template-columns: 1fr; }
          .home-modal-card { padding: 2rem 1.5rem; }
        }
      `}</style>

      <div className="home-page">
        <AppNav />

        <div className="home-content">
          {/* ─── GREETING ─── */}
          <div className="home-greeting-section">
            <div className="home-label">Your Dashboard</div>
            <h1 className="home-greeting">
              {getGreeting()}, <em>{getFirstName(userName)}.</em>
            </h1>
            <p className="home-subtitle">Here's an overview of your shared photo albums.</p>
          </div>

          {loading ? (
            /* ─── LOADING ─── */
            <div className="home-loading">
              <div className="home-spinner" />
              <div className="home-loading-text">Loading your albums...</div>
            </div>
          ) : (
            <>
              {/* ─── STATS ─── */}
              <div className="home-stats">
                <div className="home-stat-card">
                  <div className="home-stat-number">{albums.length}</div>
                  <div className="home-stat-label">Trips</div>
                </div>
                <div className="home-stat-card">
                  <div className="home-stat-number">{totalPhotos}</div>
                  <div className="home-stat-label">Photos</div>
                </div>
                <div className="home-stat-card">
                  <div className="home-stat-number">{totalCollaborators}</div>
                  <div className="home-stat-label">Collaborators</div>
                </div>
              </div>

              {/* ─── ALBUMS ─── */}
              {albums.length === 0 ? (
                <div className="home-empty">
                  <div className="home-empty-icon">
                    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <rect x="8" y="12" width="48" height="40" rx="4" />
                      <circle cx="24" cy="28" r="6" />
                      <path d="M8 44l14-12 8 6 12-10 14 12" />
                      <path d="M42 12V8a4 4 0 00-4-4H26a4 4 0 00-4 4v4" />
                    </svg>
                  </div>
                  <h2 className="home-empty-heading">No Trips Yet</h2>
                  <p className="home-empty-desc">
                    Create your first trip album to start collecting and curating photos with friends and fellow travelers.
                  </p>
                  <button className="home-cta-btn" onClick={() => setShowModal(true)}>
                    <span>Create Your First Trip</span>
                    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                      <path d="M3 8h10M9 4l4 4-4 4" />
                    </svg>
                  </button>
                </div>
              ) : (
                <>
                  <div className="home-section-header">
                    <h2 className="home-section-title">Your Trips</h2>
                    <button className="home-cta-btn" onClick={() => setShowModal(true)}>
                      <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                        <path d="M8 3v10M3 8h10" />
                      </svg>
                      <span>New Trip</span>
                    </button>
                  </div>

                  <div className="home-album-grid">
                    {albums.map((album, i) => {
                      const isOwner = album.ownerId === auth.user?.id;
                      const delay = Math.min(0.55 + i * 0.05, 1);
                      const count = mediaCounts[album.id] ?? 0;
                      return (
                        <Link
                          key={album.id}
                          to="/albums/$albumId"
                          params={{ albumId: album.id }}
                          className="home-album-card"
                          style={{ animation: `homeFadeUp 0.8s ease ${delay}s both` }}
                        >
                          {album.coverUrl ? (
                            <div className="home-album-cover">
                              <img src={album.coverUrl} alt="" loading="lazy" />
                              {count > 0 && (
                                <div className="home-album-photo-count">
                                  <svg width="10" height="10" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                                    <rect x="2" y="3" width="12" height="10" rx="1.5" />
                                    <circle cx="6" cy="7" r="1.5" />
                                    <path d="M2 11l4-3 2.5 2L12 7l2 2.5" />
                                  </svg>
                                  {count}
                                </div>
                              )}
                            </div>
                          ) : (
                            <div className="home-album-placeholder">
                              {album.title.charAt(0).toUpperCase()}
                            </div>
                          )}
                          <div className="home-album-body">
                            <h3 className="home-album-title">{album.title}</h3>
                            <p className="home-album-desc">
                              {album.description || 'No description'}
                            </p>
                            <div className="home-album-meta">
                              <span className={`home-album-badge ${isOwner ? 'home-badge-owner' : 'home-badge-member'}`}>
                                {isOwner ? 'Owner' : 'Member'}
                              </span>
                              <span className="home-album-meta-text">
                                {album.memberCount} {album.memberCount === 1 ? 'member' : 'members'}
                              </span>
                              <span className="home-album-meta-text">
                                {formatDate(album.createdAt)}
                              </span>
                            </div>
                          </div>
                        </Link>
                      );
                    })}

                    {/* New album card */}
                    <button className="home-new-album-card" onClick={() => setShowModal(true)}
                      style={{ animation: `homeFadeUp 0.8s ease ${Math.min(0.55 + albums.length * 0.05, 1)}s both` }}
                    >
                      <div className="home-new-album-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                          <path d="M12 5v14M5 12h14" />
                        </svg>
                      </div>
                      <span className="home-new-album-label">New Trip</span>
                    </button>
                  </div>
                </>
              )}
            </>
          )}
        </div>

        {/* ─── MODAL ─── */}
        {showModal && (
          <div className="home-modal-overlay" onClick={(e) => {
            if (e.target === e.currentTarget) setShowModal(false);
          }}>
            <div className="home-modal-card">
              <h2 className="home-modal-title">Create New Trip</h2>
              <div className="home-modal-field">
                <label className="home-modal-label">Trip Title</label>
                <input
                  className="home-modal-input"
                  type="text"
                  placeholder="e.g. Summer in Italy 2024"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  autoFocus
                  onKeyDown={(e) => { if (e.key === 'Enter') handleCreate(); }}
                />
              </div>
              <div className="home-modal-field">
                <label className="home-modal-label">Description (optional)</label>
                <textarea
                  className="home-modal-textarea"
                  placeholder="A brief description of this trip..."
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                />
              </div>
              <div className="home-modal-actions">
                <button className="home-modal-cancel" onClick={() => {
                  setShowModal(false);
                  setNewTitle('');
                  setNewDesc('');
                }}>Cancel</button>
                <button
                  className="home-modal-submit"
                  onClick={handleCreate}
                  disabled={!newTitle.trim() || creating}
                >
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
