import { useState, useEffect } from 'react';
import { Link, useParams } from '@tanstack/react-router';
import { getAlbum, getMembers, createInviteLink, AlbumDto, AlbumMemberDto, InviteLinkDto } from '../../api/albums';

export function AlbumDetailPage() {
  const { albumId } = useParams({ from: '/albums/$albumId' });
  const [album, setAlbum] = useState<AlbumDto | null>(null);
  const [members, setMembers] = useState<AlbumMemberDto[]>([]);
  const [inviteLink, setInviteLink] = useState<InviteLinkDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAlbum();
  }, [albumId]);

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
  );
}
