import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { apiClient } from '../api/client';
import { AppNav } from '../components/AppNav';

export function ProfilePage() {
  const auth = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [displayName, setDisplayName] = useState(auth.user?.displayName ?? '');
  const [saving, setSaving] = useState(false);

  const userName = auth.user?.displayName || 'User';
  const userInitial = userName.charAt(0).toUpperCase();

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
    <>
      <style>{`
        .profile-page {
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          min-height: 100vh;
        }

        .profile-content {
          max-width: 640px;
          margin: 0 auto;
          padding: 100px var(--space-8) var(--space-12);
        }

        .profile-page-title {
          font-family: var(--font-display);
          font-size: 1.8rem;
          font-weight: 800;
          margin: 0 0 2rem;
          color: var(--color-deep-navy);
        }

        /* Profile card */
        .profile-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.06);
          border-radius: var(--radius-lg);
          padding: 2rem;
          box-shadow: var(--shadow-md);
          margin-bottom: 2rem;
        }

        .profile-card-top {
          display: flex;
          align-items: center;
          gap: 1.5rem;
          margin-bottom: 1.5rem;
        }

        .profile-avatar {
          width: 72px;
          height: 72px;
          border-radius: var(--radius-full);
          object-fit: cover;
          border: 3px solid rgba(27,127,204,0.15);
        }

        .profile-avatar-initial {
          width: 72px;
          height: 72px;
          border-radius: var(--radius-full);
          background: rgba(27,127,204,0.08);
          border: 3px solid rgba(27,127,204,0.15);
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: 1.8rem;
          font-weight: 800;
          color: var(--color-sky-blue);
          flex-shrink: 0;
        }

        .profile-info h2 {
          font-family: var(--font-display);
          font-size: 1.4rem;
          font-weight: 800;
          margin: 0 0 0.25rem;
          color: var(--color-deep-navy);
        }

        .profile-info p {
          margin: 0;
          font-size: 0.85rem;
          color: var(--color-muted-slate);
        }

        .profile-edit-btn {
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.6rem 1.4rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-sm);
          transition: background 0.2s;
        }
        .profile-edit-btn:hover { background: var(--color-ocean-blue); }

        /* Edit form */
        .profile-edit-form {
          margin-top: 1rem;
        }

        .profile-field-label {
          display: block;
          font-size: 0.75rem;
          font-weight: 500;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          color: var(--color-muted-slate);
          margin-bottom: 0.4rem;
        }

        .profile-field-input {
          width: 100%;
          padding: 0.7rem 1rem;
          background: rgba(253,248,239,0.6);
          border: 1px solid rgba(27,127,204,0.12);
          border-radius: var(--radius-sm);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          font-size: 0.9rem;
          outline: none;
          transition: border-color 0.2s;
          box-sizing: border-box;
        }
        .profile-field-input:focus { border-color: var(--color-sky-blue); }

        .profile-form-actions {
          display: flex;
          gap: 0.75rem;
          margin-top: 1rem;
        }

        .profile-save-btn {
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          padding: 0.6rem 1.4rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-sm);
          transition: background 0.2s;
        }
        .profile-save-btn:hover { background: var(--color-ocean-blue); }
        .profile-save-btn:disabled { opacity: 0.4; cursor: not-allowed; }

        .profile-cancel-btn {
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
        .profile-cancel-btn:hover {
          color: var(--color-deep-navy);
          border-color: rgba(27,127,204,0.3);
        }

        /* Logout */
        .profile-logout-btn {
          background: transparent;
          border: 1px solid var(--color-soft-red);
          color: var(--color-soft-red);
          padding: 0.6rem 1.4rem;
          font-size: 0.8rem;
          font-weight: 600;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: all 0.2s;
          margin-top: 0.5rem;
        }
        .profile-logout-btn:hover {
          background: rgba(224,82,82,0.06);
        }

        /* Coming Soon placeholders */
        .profile-coming-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1rem;
          margin-top: 2rem;
        }

        .profile-coming-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.06);
          border-radius: var(--radius-md);
          padding: 2rem 1.5rem;
          text-align: center;
          box-shadow: var(--shadow-sm);
        }

        .profile-coming-icon {
          margin-bottom: 0.75rem;
          display: flex;
          justify-content: center;
        }

        .profile-coming-title {
          font-family: var(--font-display);
          font-size: 1rem;
          font-weight: 700;
          margin: 0 0 0.5rem;
          color: var(--color-deep-navy);
        }

        .profile-coming-badge {
          display: inline-block;
          font-size: 0.6rem;
          font-weight: 700;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          padding: 0.2rem 0.7rem;
          border-radius: var(--radius-full);
          background: rgba(244,120,32,0.1);
          color: var(--color-sunrise-orange);
        }

        .profile-coming-badge-blue {
          background: rgba(27,127,204,0.1);
          color: var(--color-sky-blue);
        }

        .profile-divider {
          border: none;
          border-top: 1px solid rgba(27,127,204,0.08);
          margin: 2rem 0;
        }

        @media (max-width: 768px) {
          .profile-content { padding: 90px var(--space-4) var(--space-6); }
          .profile-coming-grid { grid-template-columns: 1fr; }
        }
      `}</style>

      <div className="profile-page">
        <AppNav />

        <div className="profile-content">
          <h1 className="profile-page-title">Profile</h1>

          <div className="profile-card">
            <div className="profile-card-top">
              {auth.user?.avatarUrl ? (
                <img className="profile-avatar" src={auth.user.avatarUrl} alt="" />
              ) : (
                <div className="profile-avatar-initial">{userInitial}</div>
              )}
              <div className="profile-info">
                <h2>{auth.user?.displayName ?? 'Not set'}</h2>
                <p>{auth.user?.email}</p>
              </div>
            </div>

            {isEditing ? (
              <div className="profile-edit-form">
                <label className="profile-field-label">Display Name</label>
                <input
                  className="profile-field-input"
                  type="text"
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  autoFocus
                />
                <div className="profile-form-actions">
                  <button className="profile-save-btn" onClick={handleSave} disabled={saving}>
                    {saving ? 'Saving...' : 'Save'}
                  </button>
                  <button className="profile-cancel-btn" onClick={() => setIsEditing(false)}>Cancel</button>
                </div>
              </div>
            ) : (
              <button className="profile-edit-btn" onClick={() => setIsEditing(true)}>
                Edit Profile
              </button>
            )}
          </div>

          <hr className="profile-divider" />

          <button className="profile-logout-btn" onClick={auth.logout}>
            Log Out
          </button>

          {/* Coming Soon placeholders */}
          <div className="profile-coming-grid">
            <div className="profile-coming-card">
              <div className="profile-coming-icon">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#F47820" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="2" y="3" width="20" height="14" rx="2" />
                  <path d="M8 21h8" />
                  <path d="M12 17v4" />
                  <circle cx="12" cy="10" r="2" />
                </svg>
              </div>
              <div className="profile-coming-title">Your Recaps</div>
              <span className="profile-coming-badge">Coming Soon</span>
            </div>

            <div className="profile-coming-card">
              <div className="profile-coming-icon">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#1B7FCC" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
                </svg>
              </div>
              <div className="profile-coming-title">Trip Activity</div>
              <span className="profile-coming-badge profile-coming-badge-blue">Coming Soon</span>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
