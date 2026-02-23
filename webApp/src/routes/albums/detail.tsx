import { useState, useEffect, useRef } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { getAlbum, getMembers, createInviteLink, AlbumDto, AlbumMemberDto, InviteLinkDto } from '../../api/albums';
import { getAlbumMedia, MediaDto } from '../../api/media';
import { useUploadManager } from '../../hooks/useUploadManager';
import { UploadZone } from '../../components/UploadZone';
import { UploadProgress } from '../../components/UploadProgress';
import { MediaGrid } from '../../components/MediaGrid';
import { Lightbox } from '../../components/Lightbox';

export function AlbumDetailPage() {
  const { albumId } = useParams({ from: '/albums/$albumId' });
  const [album, setAlbum] = useState<AlbumDto | null>(null);
  const [members, setMembers] = useState<AlbumMemberDto[]>([]);
  const [media, setMedia] = useState<MediaDto[]>([]);
  const [inviteLink, setInviteLink] = useState<InviteLinkDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);

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

  // Check if current user can upload (owner or contributor)
  // For now, show upload zone to all members — server will enforce permissions
  const canUpload = album != null;

  if (loading) {
    return <div style={{ padding: '2rem' }}>Loading...</div>;
  }

  if (!album) {
    return <div style={{ padding: '2rem' }}>Album not found.</div>;
  }

  return (
    <div style={{ padding: '2rem', maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
        <Link to="/albums">&larr; Back</Link>
        <h1>{album.title}</h1>
      </div>

      {album.description && (
        <p style={{ color: '#666', marginBottom: '1.5rem' }}>{album.description}</p>
      )}

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
          onClose={() => setLightboxIndex(null)}
          onNavigate={setLightboxIndex}
        />
      )}

      {/* Members section */}
      <div style={{ marginTop: '2rem' }}>
        <h2>Members ({members.length})</h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.5rem' }}>
          {members.map((member) => (
            <div
              key={member.userId}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                border: '1px solid #ccc',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
              }}
            >
              <span>{member.displayName}</span>
              <span style={{ color: '#999', fontSize: '0.875rem' }}>
                {member.role.charAt(0) + member.role.slice(1).toLowerCase()}
              </span>
            </div>
          ))}
        </div>

        <button onClick={handleCreateInvite}>Generate Invite Link</button>

        {inviteLink && (
          <div style={{ marginTop: '1rem', border: '1px solid #ccc', padding: '1rem', borderRadius: '8px' }}>
            <strong>Invite Token:</strong>
            <p style={{ fontFamily: 'monospace', wordBreak: 'break-all', margin: '0.5rem 0 0' }}>
              {inviteLink.token}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
