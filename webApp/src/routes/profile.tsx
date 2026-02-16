import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../api/client';

export function ProfilePage() {
  const auth = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [displayName, setDisplayName] = useState(auth.user?.displayName ?? '');
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      const data = await apiClient('/api/profile', {
        method: 'PUT',
        body: JSON.stringify({ displayName }),
      });
      if (data.success && data.data) {
        auth.login(
          localStorage.getItem('thykra_access_token')!,
          localStorage.getItem('thykra_refresh_token')!,
          data.data
        );
        setIsEditing(false);
      }
    } catch (error) {
      console.error('Failed to update profile:', error);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '600px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '2rem' }}>
        <Link to="/">&larr; Back</Link>
        <h1>Profile</h1>
      </div>
      {isEditing ? (
        <div>
          <label>
            Display Name
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              style={{ display: 'block', width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
            />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
            <button onClick={handleSave} disabled={saving}>
              {saving ? 'Saving...' : 'Save'}
            </button>
            <button onClick={() => setIsEditing(false)}>Cancel</button>
          </div>
        </div>
      ) : (
        <div>
          <p><strong>Name:</strong> {auth.user?.displayName ?? 'Not set'}</p>
          <p><strong>Email:</strong> {auth.user?.email}</p>
          <button onClick={() => setIsEditing(true)} style={{ marginTop: '1rem' }}>
            Edit Profile
          </button>
        </div>
      )}
      <hr style={{ margin: '2rem 0' }} />
      <button onClick={auth.logout} style={{ color: 'red' }}>
        Logout
      </button>
    </div>
  );
}
