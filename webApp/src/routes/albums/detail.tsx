import { useState, useEffect, useRef } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { getAlbum, getMembers, createInviteLink, AlbumDto, AlbumMemberDto, InviteLinkDto } from '../../api/albums';
import { getAlbumMedia, MediaDto } from '../../api/media';
import { useUploadManager } from '../../hooks/useUploadManager';
import { UploadZone } from '../../components/UploadZone';
import { UploadProgress } from '../../components/UploadProgress';
import { MediaGrid } from '../../components/MediaGrid';
import { Lightbox } from '../../components/Lightbox';
import { AppNav } from '../../components/AppNav';
import { useAuth } from '../../auth/AuthContext';

export function AlbumDetailPage() {
  const { albumId } = useParams({ from: '/albums/$albumId' });
  const { user } = useAuth();
  const [album, setAlbum] = useState<AlbumDto | null>(null);
  const [members, setMembers] = useState<AlbumMemberDto[]>([]);
  const [media, setMedia] = useState<MediaDto[]>([]);
  const [inviteLink, setInviteLink] = useState<InviteLinkDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);

  const { uploads, enqueue, clearCompleted } = useUploadManager();
  const prevDoneCountRef = useRef(0);

  useEffect(() => {
    loadAlbum();
    loadMedia();
  }, [albumId]);

  // Auto-refresh media when uploads complete
  useEffect(() => {
    const doneCount = uploads.filter((u) => u.albumId === albumId && u.status === 'DONE').length;
    if (doneCount > prevDoneCountRef.current) {
      loadMedia();
      clearCompleted();
    }
    prevDoneCountRef.current = doneCount;
  }, [uploads, albumId]);

  async function loadAlbum() {
    setLoading(true);
    try {
      const albumData = await getAlbum(albumId);
      if (albumData.success && albumData.data) {
        setAlbum(albumData.data);
      }
      const membersData = await getMembers(albumId);
      if (membersData.success && membersData.data) {
        setMembers(membersData.data);
      }
    } catch (error) {
      console.error('Failed to load album:', error);
    } finally {
      setLoading(false);
    }
  }

  async function loadMedia() {
    try {
      const resp = await getAlbumMedia(albumId);
      if (resp.success && resp.data) {
        setMedia(resp.data);
      }
    } catch (error) {
      console.error('Failed to load media:', error);
    }
  }

  async function handleCreateInvite() {
    try {
      const data = await createInviteLink(albumId);
      if (data.success && data.data) {
        setInviteLink(data.data);
      }
    } catch (error) {
      console.error('Failed to create invite link:', error);
    }
  }

  function handleFilesSelected(files: File[]) {
    enqueue(files, albumId);
  }

  function handleCopyToken() {
    if (!inviteLink) return;
    navigator.clipboard.writeText(inviteLink.token);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  // Check if current user can upload (owner or contributor)
  // For now, show upload zone to all members — server will enforce permissions
  const canUpload = album != null;

  return (
    <>
      <style>{`
        .detail-page {
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          min-height: 100vh;
        }

        .detail-content {
          max-width: 900px;
          margin: 0 auto;
          padding: 100px var(--space-8) var(--space-12);
        }

        .detail-breadcrumb {
          display: inline-flex;
          align-items: center;
          gap: 0.5rem;
          color: var(--color-sky-blue);
          text-decoration: none;
          font-size: 0.85rem;
          font-weight: 500;
          margin-bottom: 1.5rem;
          transition: opacity 0.2s;
        }
        .detail-breadcrumb:hover { opacity: 0.7; }

        .detail-header {
          margin-bottom: 2rem;
        }

        .detail-title {
          font-family: var(--font-display);
          font-size: 2rem;
          font-weight: 800;
          margin: 0 0 0.4rem;
          color: var(--color-deep-navy);
        }

        .detail-desc {
          font-size: 0.9rem;
          color: var(--color-muted-slate);
          margin: 0 0 0.5rem;
          line-height: 1.6;
        }

        .detail-meta {
          font-size: 0.8rem;
          color: var(--color-muted-slate);
          font-weight: 500;
        }

        /* Members */
        .detail-section-title {
          font-family: var(--font-display);
          font-size: 1.2rem;
          font-weight: 700;
          margin: 2.5rem 0 1rem;
          color: var(--color-deep-navy);
        }

        .detail-member-list {
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
          margin-bottom: 1.5rem;
        }

        .detail-member-card {
          display: flex;
          justify-content: space-between;
          align-items: center;
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.06);
          padding: 0.75rem 1rem;
          border-radius: var(--radius-sm);
          box-shadow: var(--shadow-sm);
        }

        .detail-member-name {
          font-weight: 500;
          font-size: 0.9rem;
          color: var(--color-deep-navy);
        }

        .detail-member-role {
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          padding: 0.2rem 0.6rem;
          border-radius: var(--radius-full);
        }
        .detail-role-owner {
          background: rgba(27,127,204,0.1);
          color: var(--color-sky-blue);
        }
        .detail-role-member {
          background: var(--color-sandy);
          color: var(--color-muted-slate);
          border: 1px solid rgba(27,127,204,0.1);
        }

        /* Invite */
        .detail-invite-btn {
          display: inline-flex;
          align-items: center;
          gap: 0.5rem;
          background: transparent;
          border: 1px solid var(--color-sky-blue);
          color: var(--color-sky-blue);
          padding: 0.6rem 1.4rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: all 0.2s;
        }
        .detail-invite-btn:hover {
          background: rgba(27,127,204,0.06);
        }

        .detail-invite-card {
          margin-top: 1rem;
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.12);
          padding: 1rem 1.2rem;
          border-radius: var(--radius-sm);
          box-shadow: var(--shadow-sm);
        }

        .detail-invite-label {
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          color: var(--color-muted-slate);
          margin-bottom: 0.5rem;
        }

        .detail-invite-token {
          font-family: monospace;
          font-size: 0.8rem;
          word-break: break-all;
          color: var(--color-deep-navy);
          background: rgba(253,248,239,0.6);
          padding: 0.5rem 0.75rem;
          border-radius: var(--radius-sm);
          border: 1px solid rgba(27,127,204,0.08);
          margin-bottom: 0.5rem;
        }

        .detail-copy-btn {
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.4rem 1rem;
          font-size: 0.75rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: background 0.2s;
        }
        .detail-copy-btn:hover { background: var(--color-ocean-blue); }

        /* Loading / Error */
        .detail-loading {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 8rem 2rem;
          gap: 1.5rem;
        }

        .detail-spinner {
          width: 32px;
          height: 32px;
          border: 2px solid rgba(27,127,204,0.15);
          border-top-color: var(--color-sky-blue);
          border-radius: 50%;
          animation: detailSpin 0.8s linear infinite;
        }

        .detail-loading-text {
          font-size: 0.85rem;
          color: var(--color-muted-slate);
          font-weight: 500;
        }

        .detail-error {
          text-align: center;
          padding: 6rem 2rem;
          color: var(--color-muted-slate);
          font-size: 1rem;
        }

        @keyframes detailSpin {
          to { transform: rotate(360deg); }
        }

        @media (max-width: 768px) {
          .detail-content { padding: 90px var(--space-4) var(--space-6); }
          .detail-title { font-size: 1.6rem; }
        }
      `}</style>

      <div className="detail-page">
        <AppNav />

        <div className="detail-content">
          {loading ? (
            <div className="detail-loading">
              <div className="detail-spinner" />
              <div className="detail-loading-text">Loading album...</div>
            </div>
          ) : !album ? (
            <div className="detail-error">
              Album not found.{' '}
              <Link to="/" style={{ color: 'var(--color-sky-blue)' }}>Go back home</Link>
            </div>
          ) : (
            <>
              <Link to="/" className="detail-breadcrumb">
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M13 8H3M7 4l-4 4 4 4" />
                </svg>
                Back to Trips
              </Link>

              <div className="detail-header">
                <h1 className="detail-title">{album.title}</h1>
                {album.description && (
                  <p className="detail-desc">{album.description}</p>
                )}
                <div className="detail-meta">
                  {members.length} {members.length === 1 ? 'member' : 'members'} &middot; {media.length} {media.length === 1 ? 'photo' : 'photos'}
                </div>
              </div>

              {/* Upload zone */}
              {canUpload && <UploadZone onFilesSelected={handleFilesSelected} />}

              {/* Upload progress */}
              <UploadProgress uploads={uploads} albumId={albumId} />

              {/* Media grid */}
              <MediaGrid media={media} onSelect={(index) => setLightboxIndex(index)} />

              {/* Lightbox */}
              {lightboxIndex !== null && (
                <Lightbox
                  media={media}
                  currentIndex={lightboxIndex}
                  albumId={albumId}
                  currentUserId={user?.id ?? null}
                  albumOwnerId={album?.ownerId ?? null}
                  onClose={() => setLightboxIndex(null)}
                  onNavigate={setLightboxIndex}
                />
              )}

              {/* Members section */}
              <h2 className="detail-section-title">Members ({members.length})</h2>
              <div className="detail-member-list">
                {members.map((member) => {
                  const roleKey = member.role.toLowerCase();
                  return (
                    <div key={member.userId} className="detail-member-card">
                      <span className="detail-member-name">{member.displayName}</span>
                      <span className={`detail-member-role ${roleKey === 'owner' ? 'detail-role-owner' : 'detail-role-member'}`}>
                        {member.role.charAt(0) + member.role.slice(1).toLowerCase()}
                      </span>
                    </div>
                  );
                })}
              </div>

              <button className="detail-invite-btn" onClick={handleCreateInvite}>
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M6 3H3a1 1 0 00-1 1v9a1 1 0 001 1h9a1 1 0 001-1v-3" />
                  <path d="M9 1h6v6" />
                  <path d="M15 1L7 9" />
                </svg>
                Generate Invite Link
              </button>

              {inviteLink && (
                <div className="detail-invite-card">
                  <div className="detail-invite-label">Invite Token</div>
                  <div className="detail-invite-token">{inviteLink.token}</div>
                  <button className="detail-copy-btn" onClick={handleCopyToken}>
                    {copied ? 'Copied!' : 'Copy Token'}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  );
}
