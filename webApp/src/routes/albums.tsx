import { useState, useEffect } from 'react';
import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { getAlbums, createAlbum, AlbumDto } from '../api/albums';

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

  return (
    <div style={{ padding: '2rem', maxWidth: '800px', margin: '0 auto' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Albums</h1>
        <nav style={{ display: 'flex', gap: '1rem' }}>
          <Link to="/profile">Profile</Link>
          <button onClick={auth.logout}>Logout</button>
        </nav>
      </header>

      <button onClick={() => setShowCreate(true)} style={{ marginBottom: '1rem' }}>
        + Create Album
      </button>

      {showCreate && (
        <div style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
          <h3>Create Album</h3>
          <label style={{ display: 'block', marginBottom: '0.5rem' }}>
            Title
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{ display: 'block', width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
            />
          </label>
          <label style={{ display: 'block', marginBottom: '0.5rem' }}>
            Description (optional)
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              style={{ display: 'block', width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
            />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button onClick={handleCreate} disabled={creating || !title.trim()}>
              {creating ? 'Creating...' : 'Create'}
            </button>
            <button onClick={() => setShowCreate(false)}>Cancel</button>
          </div>
        </div>
      )}

      {loading ? (
        <p>Loading albums...</p>
      ) : albums.length === 0 ? (
        <p>No albums yet. Create one to get started.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {albums.map((album) => (
            <Link
              key={album.id}
              to="/albums/$albumId"
              params={{ albumId: album.id }}
              style={{ textDecoration: 'none', color: 'inherit' }}
            >
              <div style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: '8px', cursor: 'pointer' }}>
                <strong>{album.title}</strong>
                {album.description && <p style={{ margin: '0.25rem 0 0', color: '#666' }}>{album.description}</p>}
                <p style={{ margin: '0.25rem 0 0', color: '#999', fontSize: '0.875rem' }}>
                  {album.memberCount} member{album.memberCount !== 1 ? 's' : ''}
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
