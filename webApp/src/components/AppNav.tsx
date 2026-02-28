import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';

export function AppNav() {
  const auth = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const userName = auth.user?.displayName || 'User';
  const userInitial = userName.charAt(0).toUpperCase();

  return (
    <>
      <style>{`
        .app-nav {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 var(--space-8);
          height: 64px;
          z-index: 50;
          background: rgba(253, 248, 239, 0.92);
          backdrop-filter: blur(12px);
          -webkit-backdrop-filter: blur(12px);
          border-bottom: 1px solid rgba(27, 127, 204, 0.08);
          box-shadow: 0 1px 8px rgba(27, 127, 204, 0.06);
        }

        .app-nav-left {
          display: flex;
          align-items: center;
          gap: var(--space-8);
        }

        .app-nav-logo {
          font-family: var(--font-display);
          font-size: 1.4rem;
          font-weight: 800;
          color: var(--color-deep-navy);
          text-decoration: none;
          letter-spacing: 0.02em;
        }

        .app-nav-center {
          display: flex;
          gap: var(--space-6);
        }

        .app-nav-link {
          font-family: var(--font-body);
          font-size: 0.85rem;
          font-weight: 500;
          color: var(--color-muted-slate);
          text-decoration: none;
          transition: color 0.2s;
          position: relative;
          padding: 0.25rem 0;
        }
        .app-nav-link:hover { color: var(--color-sky-blue); }
        .app-nav-link.active {
          color: var(--color-sky-blue);
        }
        .app-nav-link.active::after {
          content: '';
          position: absolute;
          bottom: -2px;
          left: 0;
          right: 0;
          height: 2px;
          background: var(--color-sky-blue);
          border-radius: 1px;
        }

        .app-nav-link-coming {
          font-family: var(--font-body);
          font-size: 0.85rem;
          font-weight: 500;
          color: var(--color-muted-slate);
          text-decoration: none;
          opacity: 0.5;
          cursor: default;
          display: flex;
          align-items: center;
          gap: 0.4rem;
        }

        .app-nav-coming-badge {
          font-size: 0.55rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          background: var(--color-sandy);
          color: var(--color-sunrise-orange);
          padding: 0.15rem 0.45rem;
          border-radius: var(--radius-full);
        }

        .app-nav-right {
          display: flex;
          align-items: center;
          gap: var(--space-4);
        }

        .app-nav-user {
          display: flex;
          align-items: center;
          gap: 0.6rem;
        }

        .app-nav-avatar {
          width: 32px;
          height: 32px;
          border-radius: var(--radius-full);
          object-fit: cover;
          border: 2px solid var(--color-sandy);
        }

        .app-nav-initial {
          width: 32px;
          height: 32px;
          border-radius: var(--radius-full);
          background: var(--color-sandy);
          border: 2px solid rgba(27, 127, 204, 0.15);
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-display);
          font-size: 0.85rem;
          font-weight: 700;
          color: var(--color-sky-blue);
        }

        .app-nav-username {
          font-size: 0.8rem;
          font-weight: 500;
          color: var(--color-deep-navy);
        }

        .app-nav-logout {
          background: none;
          border: 1px solid rgba(27, 127, 204, 0.2);
          color: var(--color-muted-slate);
          padding: 0.4rem 1rem;
          font-size: 0.75rem;
          font-weight: 500;
          cursor: pointer;
          font-family: var(--font-body);
          border-radius: var(--radius-sm);
          transition: all 0.2s;
        }
        .app-nav-logout:hover {
          color: var(--color-sky-blue);
          border-color: var(--color-sky-blue);
        }

        /* Hamburger (mobile) */
        .app-nav-hamburger {
          display: none;
          background: none;
          border: none;
          cursor: pointer;
          padding: 0.5rem;
          color: var(--color-deep-navy);
        }

        /* Mobile menu */
        .app-nav-mobile-menu {
          display: none;
          position: fixed;
          top: 64px;
          left: 0;
          right: 0;
          background: rgba(253, 248, 239, 0.98);
          backdrop-filter: blur(12px);
          padding: var(--space-4) var(--space-8);
          border-bottom: 1px solid rgba(27, 127, 204, 0.08);
          box-shadow: var(--shadow-md);
          flex-direction: column;
          gap: var(--space-3);
          z-index: 49;
        }
        .app-nav-mobile-menu.open { display: flex; }

        .app-nav-mobile-link {
          font-family: var(--font-body);
          font-size: 0.9rem;
          font-weight: 500;
          color: var(--color-deep-navy);
          text-decoration: none;
          padding: 0.6rem 0;
          border-bottom: 1px solid rgba(27, 127, 204, 0.06);
        }

        @media (max-width: 768px) {
          .app-nav { padding: 0 var(--space-4); }
          .app-nav-center { display: none; }
          .app-nav-username { display: none; }
          .app-nav-hamburger { display: block; }
          .app-nav-mobile-menu { display: none; }
          .app-nav-mobile-menu.open { display: flex; }
        }
      `}</style>

      <nav className="app-nav">
        <div className="app-nav-left">
          <Link to="/" className="app-nav-logo">Thykra</Link>
          <div className="app-nav-center">
            <Link to="/" className="app-nav-link" activeOptions={{ exact: true }}>Trips</Link>
            <span className="app-nav-link-coming">
              Recaps
              <span className="app-nav-coming-badge">Soon</span>
            </span>
          </div>
        </div>

        <div className="app-nav-right">
          <Link to="/profile" className="app-nav-user" style={{ textDecoration: 'none' }}>
            {auth.user?.avatarUrl ? (
              <img className="app-nav-avatar" src={auth.user.avatarUrl} alt="" />
            ) : (
              <div className="app-nav-initial">{userInitial}</div>
            )}
            <span className="app-nav-username">{auth.user?.displayName}</span>
          </Link>
          <button className="app-nav-logout" onClick={auth.logout}>Logout</button>

          <button className="app-nav-hamburger" onClick={() => setMenuOpen(!menuOpen)}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              {menuOpen ? (
                <><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></>
              ) : (
                <><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></>
              )}
            </svg>
          </button>
        </div>
      </nav>

      <div className={`app-nav-mobile-menu${menuOpen ? ' open' : ''}`}>
        <Link to="/" className="app-nav-mobile-link" onClick={() => setMenuOpen(false)}>Trips</Link>
        <Link to="/profile" className="app-nav-mobile-link" onClick={() => setMenuOpen(false)}>Profile</Link>
      </div>
    </>
  );
}
