import { useState, useEffect, useRef } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import {
  getAlbum,
  getMembers,
  createInviteLink,
  updateAlbum,
  AlbumDto,
  AlbumMemberDto,
  AlbumVisibility,
  InviteLinkDto,
} from '../../api/albums';
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

  const [visibilitySaving, setVisibilitySaving] = useState(false);
  const [visibilityError, setVisibilityError] = useState('');
  const [inviteExpiryDays, setInviteExpiryDays] = useState<number>(7);
  const [inviteCopied, setInviteCopied] = useState(false);
  const [shareCopied, setShareCopied] = useState(false);
  const [shareTooltipOpen, setShareTooltipOpen] = useState(false);

  const { uploads, enqueue, clearCompleted } = useUploadManager();
  const prevDoneCountRef = useRef(0);

  const isOwner = album != null && user != null && album.ownerId === user.id;

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

  async function handleSetVisibility(next: AlbumVisibility) {
    if (!album || album.visibility === next) return;
    setVisibilitySaving(true);
    setVisibilityError('');
    try {
      const resp = await updateAlbum(albumId, { visibility: next });
      if (resp.success && resp.data) {
        setAlbum(resp.data);
      } else {
        setVisibilityError(resp.error || 'Failed to update visibility');
      }
    } catch (err) {
      console.error('Failed to update visibility:', err);
      setVisibilityError('Network error. Please try again.');
    } finally {
      setVisibilitySaving(false);
    }
  }

  async function handleCreateInvite() {
    try {
      const data = await createInviteLink(albumId, inviteExpiryDays);
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

  function inviteUrl(token: string): string {
    return `${window.location.origin}/invite/${token}`;
  }

  function handleCopyInvite() {
    if (!inviteLink) return;
    navigator.clipboard.writeText(inviteUrl(inviteLink.token));
    setInviteCopied(true);
    setTimeout(() => setInviteCopied(false), 2000);
  }

  function handleShare() {
    if (!album) return;
    if (album.visibility !== 'LINK_SHARED') {
      setShareTooltipOpen(true);
      setTimeout(() => setShareTooltipOpen(false), 2400);
      return;
    }
    const url = `${window.location.origin}/public/${album.id}`;
    navigator.clipboard.writeText(url);
    setShareCopied(true);
    setTimeout(() => setShareCopied(false), 2000);
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

        /* Privacy / visibility */
        .detail-visibility-options {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 0.75rem;
          margin-bottom: 0.75rem;
        }

        .detail-visibility-option {
          background: var(--color-sandy);
          border: 1.5px solid rgba(27,127,204,0.08);
          border-radius: var(--radius-sm);
          padding: 1rem 1.1rem;
          text-align: left;
          cursor: pointer;
          font-family: var(--font-body);
          color: var(--color-deep-navy);
          transition: all 0.18s;
          display: flex;
          flex-direction: column;
          gap: 0.35rem;
        }
        .detail-visibility-option:hover:not(:disabled) {
          border-color: rgba(27,127,204,0.3);
          background: rgba(27,127,204,0.04);
        }
        .detail-visibility-option:disabled {
          cursor: not-allowed;
          opacity: 0.6;
        }
        .detail-visibility-option.active {
          background: rgba(27,127,204,0.08);
          border-color: var(--color-sky-blue);
          box-shadow: var(--shadow-sm);
        }
        .detail-visibility-title {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          font-size: 0.9rem;
          font-weight: 700;
          color: var(--color-deep-navy);
        }
        .detail-visibility-desc {
          font-size: 0.78rem;
          color: var(--color-muted-slate);
          line-height: 1.45;
        }
        .detail-visibility-check {
          margin-left: auto;
          color: var(--color-sky-blue);
        }
        .detail-visibility-error {
          font-size: 0.78rem;
          color: var(--color-soft-red);
          margin-top: 0.4rem;
        }

        @media (max-width: 600px) {
          .detail-visibility-options { grid-template-columns: 1fr; }
        }

        /* Invite expiry chips */
        .detail-expiry-row {
          display: flex;
          flex-wrap: wrap;
          align-items: center;
          gap: 0.5rem;
          margin-bottom: 0.85rem;
        }
        .detail-expiry-label {
          font-size: 0.78rem;
          font-weight: 600;
          color: var(--color-muted-slate);
          margin-right: 0.25rem;
        }
        .detail-expiry-chip {
          background: transparent;
          border: 1px solid rgba(27,127,204,0.18);
          color: var(--color-muted-slate);
          padding: 0.32rem 0.85rem;
          font-size: 0.78rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-full);
          transition: all 0.18s;
        }
        .detail-expiry-chip:hover {
          border-color: var(--color-sky-blue);
          color: var(--color-sky-blue);
        }
        .detail-expiry-chip.active {
          background: var(--color-sky-blue);
          border-color: var(--color-sky-blue);
          color: #fff;
        }
        .detail-invite-expiry {
          font-size: 0.72rem;
          color: var(--color-muted-slate);
          margin-bottom: 0.5rem;
        }

        /* Share button + tooltip */
        .detail-title-row {
          display: flex;
          align-items: flex-start;
          gap: 0.75rem;
          flex-wrap: wrap;
        }
        .detail-title-row .detail-title { flex: 1; min-width: 0; }

        .detail-share-wrapper {
          position: relative;
          margin-top: 0.4rem;
        }
        .detail-share-btn {
          display: inline-flex;
          align-items: center;
          gap: 0.45rem;
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.5rem 1rem;
          font-size: 0.78rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: background 0.2s, opacity 0.2s;
        }
        .detail-share-btn:hover { background: var(--color-ocean-blue); }
        .detail-share-btn.disabled {
          background: var(--color-sandy);
          color: var(--color-muted-slate);
          border: 1px solid rgba(27,127,204,0.15);
        }
        .detail-share-btn.disabled:hover { background: var(--color-sandy); }

        .detail-share-tooltip {
          position: absolute;
          top: calc(100% + 8px);
          right: 0;
          background: var(--color-deep-navy);
          color: #fff;
          font-size: 0.74rem;
          padding: 0.55rem 0.8rem;
          border-radius: var(--radius-sm);
          box-shadow: var(--shadow-md);
          width: max-content;
          max-width: 240px;
          z-index: 10;
          line-height: 1.4;
        }
        .detail-share-tooltip::before {
          content: '';
          position: absolute;
          top: -5px;
          right: 18px;
          border: 5px solid transparent;
          border-top: 0;
          border-bottom-color: var(--color-deep-navy);
        }

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
                <div className="detail-title-row">
                  <h1 className="detail-title">{album.title}</h1>
                  {isOwner && (
                    <div className="detail-share-wrapper">
                      <button
                        type="button"
                        className={`detail-share-btn${album.visibility !== 'LINK_SHARED' ? ' disabled' : ''}`}
                        onClick={handleShare}
                        title={album.visibility === 'LINK_SHARED' ? 'Copy public share link' : 'Album is private'}
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                          <circle cx="12" cy="3" r="2" />
                          <circle cx="4" cy="8" r="2" />
                          <circle cx="12" cy="13" r="2" />
                          <path d="M5.7 7l4.6-2.7M5.7 9l4.6 2.7" />
                        </svg>
                        {shareCopied ? 'Link copied!' : 'Share'}
                      </button>
                      {shareTooltipOpen && (
                        <div className="detail-share-tooltip" role="status">
                          This album is private. Switch to <strong>Anyone with link</strong> in Privacy to share publicly.
                        </div>
                      )}
                    </div>
                  )}
                </div>
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

              {/* Privacy section (owner only) */}
              {isOwner && (
                <>
                  <h2 className="detail-section-title">Privacy</h2>
                  <div className="detail-visibility-options">
                    <button
                      type="button"
                      className={`detail-visibility-option${album.visibility === 'PRIVATE' ? ' active' : ''}`}
                      onClick={() => handleSetVisibility('PRIVATE')}
                      disabled={visibilitySaving}
                    >
                      <span className="detail-visibility-title">
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6">
                          <rect x="3" y="7" width="10" height="7" rx="1.2" />
                          <path d="M5 7V5a3 3 0 016 0v2" />
                        </svg>
                        Private
                        {album.visibility === 'PRIVATE' && (
                          <svg className="detail-visibility-check" width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M3 8l3.5 3.5L13 5" />
                          </svg>
                        )}
                      </span>
                      <span className="detail-visibility-desc">
                        Only members can view. Invite link is required to join.
                      </span>
                    </button>
                    <button
                      type="button"
                      className={`detail-visibility-option${album.visibility === 'LINK_SHARED' ? ' active' : ''}`}
                      onClick={() => handleSetVisibility('LINK_SHARED')}
                      disabled={visibilitySaving}
                    >
                      <span className="detail-visibility-title">
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6">
                          <path d="M7 9.5a2.5 2.5 0 003.5 0l2-2a2.5 2.5 0 00-3.5-3.5l-1 1" />
                          <path d="M9 6.5a2.5 2.5 0 00-3.5 0l-2 2a2.5 2.5 0 003.5 3.5l1-1" />
                        </svg>
                        Anyone with link
                        {album.visibility === 'LINK_SHARED' && (
                          <svg className="detail-visibility-check" width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M3 8l3.5 3.5L13 5" />
                          </svg>
                        )}
                      </span>
                      <span className="detail-visibility-desc">
                        Anyone with the share link can view this album, no sign-in needed.
                      </span>
                    </button>
                  </div>
                  {visibilityError && (
                    <div className="detail-visibility-error">{visibilityError}</div>
                  )}
                </>
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

              <h2 className="detail-section-title">Invite link</h2>
              <div className="detail-expiry-row">
                <span className="detail-expiry-label">Expires in:</span>
                {[1, 7, 30, 90].map((days) => (
                  <button
                    key={days}
                    type="button"
                    className={`detail-expiry-chip${inviteExpiryDays === days ? ' active' : ''}`}
                    onClick={() => setInviteExpiryDays(days)}
                  >
                    {days === 1 ? '1 day' : `${days} days`}
                  </button>
                ))}
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
                  <div className="detail-invite-label">Invite link</div>
                  <div className="detail-invite-token">{inviteUrl(inviteLink.token)}</div>
                  <div className="detail-invite-expiry">
                    Expires {new Date(inviteLink.expiresAt).toLocaleString()}
                  </div>
                  <button className="detail-copy-btn" onClick={handleCopyInvite}>
                    {inviteCopied ? 'Copied!' : 'Copy link'}
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
