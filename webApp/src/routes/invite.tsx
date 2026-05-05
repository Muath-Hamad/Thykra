import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { joinByInvite, AlbumDto } from '../api/albums';

type Phase = 'joining' | 'success' | 'error' | 'unauthenticated';

interface JoinResponse {
  success: boolean;
  data?: AlbumDto;
  error?: string;
}

export const PENDING_INVITE_KEY = 'thykra_pending_invite_token';

export function InvitePage() {
  const { token } = useParams({ from: '/invite/$token' });
  const auth = useAuth();
  const navigate = useNavigate();

  const [phase, setPhase] = useState<Phase>('joining');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    // Wait for auth context to finish bootstrapping before deciding what to do.
    if (auth.isLoading) return;

    if (!auth.isAuthenticated) {
      // Stash the token so the post-login flow can resume the join.
      try {
        localStorage.setItem(PENDING_INVITE_KEY, token);
      } catch {
        // localStorage unavailable (private mode quota etc.) — fall through.
      }
      setPhase('unauthenticated');
      return;
    }

    let cancelled = false;
    setPhase('joining');
    setErrorMsg(null);

    (async () => {
      try {
        const resp = (await joinByInvite(token)) as JoinResponse;
        if (cancelled) return;
        if (resp.success && resp.data) {
          // Clear any stashed token now that we've consumed it.
          try {
            localStorage.removeItem(PENDING_INVITE_KEY);
          } catch {
            /* ignore */
          }
          setPhase('success');
          navigate({
            to: '/albums/$albumId',
            params: { albumId: resp.data.id },
            replace: true,
          });
        } else {
          setErrorMsg(resp.error ?? 'This invite link could not be used.');
          setPhase('error');
        }
      } catch (err) {
        if (cancelled) return;
        console.error('Join by invite failed:', err);
        setErrorMsg('Connection error. Please try again.');
        setPhase('error');
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [auth.isAuthenticated, auth.isLoading, token, navigate]);

  return (
    <>
      <style>{`
        .inv-page {
          min-height: 100vh;
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          display: flex;
          flex-direction: column;
        }

        .inv-topbar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 var(--space-8);
          height: 60px;
          border-bottom: 1px solid rgba(27, 127, 204, 0.08);
        }
        .inv-logo {
          font-family: var(--font-display);
          font-size: 1.25rem;
          font-weight: 800;
          color: var(--color-deep-navy);
          text-decoration: none;
          letter-spacing: 0.02em;
        }

        .inv-main {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: var(--space-8);
        }

        .inv-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27, 127, 204, 0.12);
          border-radius: var(--radius-md);
          padding: 3rem 2.5rem;
          max-width: 460px;
          width: 100%;
          text-align: center;
          box-shadow: var(--shadow-md);
        }

        .inv-icon {
          width: 56px;
          height: 56px;
          border-radius: var(--radius-full);
          background: rgba(27, 127, 204, 0.1);
          border: 1px solid rgba(27, 127, 204, 0.2);
          display: flex;
          align-items: center;
          justify-content: center;
          margin: 0 auto 1.5rem;
          color: var(--color-sky-blue);
        }

        .inv-title {
          font-family: var(--font-display);
          font-size: 1.6rem;
          font-weight: 800;
          margin: 0 0 0.6rem;
          line-height: 1.2;
        }

        .inv-subtitle {
          font-size: 0.95rem;
          color: var(--color-muted-slate);
          line-height: 1.6;
          margin: 0 0 1.8rem;
        }

        .inv-btn {
          display: inline-block;
          background: var(--color-sky-blue);
          color: #fff;
          font-family: var(--font-body);
          font-size: 0.85rem;
          font-weight: 600;
          letter-spacing: 0.02em;
          padding: 0.8rem 1.8rem;
          border: none;
          border-radius: var(--radius-full);
          text-decoration: none;
          cursor: pointer;
          transition: background 0.2s, transform 0.15s;
        }
        .inv-btn:hover {
          background: #156aab;
          transform: translateY(-1px);
        }

        .inv-btn-secondary {
          display: inline-block;
          margin-top: 1rem;
          color: var(--color-muted-slate);
          font-size: 0.8rem;
          text-decoration: none;
          font-weight: 500;
        }
        .inv-btn-secondary:hover { color: var(--color-sky-blue); }

        .inv-spinner {
          width: 36px;
          height: 36px;
          border: 2px solid rgba(27,127,204,0.15);
          border-top-color: var(--color-sky-blue);
          border-radius: 50%;
          animation: invSpin 0.8s linear infinite;
          margin: 0 auto 1.5rem;
        }
        @keyframes invSpin { to { transform: rotate(360deg); } }

        .inv-error-icon {
          color: var(--color-soft-red);
          background: rgba(224, 82, 82, 0.08);
          border-color: rgba(224, 82, 82, 0.25);
        }
      `}</style>

      <div className="inv-page">
        <div className="inv-topbar">
          <Link to="/" className="inv-logo">Thykra</Link>
          {!auth.isAuthenticated && (
            <Link to="/login" className="inv-logo" style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--color-sky-blue)' }}>
              Sign in
            </Link>
          )}
        </div>

        <div className="inv-main">
          {phase === 'joining' && (
            <div className="inv-card">
              <div className="inv-spinner" />
              <h1 className="inv-title">Joining album...</h1>
              <p className="inv-subtitle">Hang tight while we add you to this album.</p>
            </div>
          )}

          {phase === 'success' && (
            <div className="inv-card">
              <div className="inv-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M5 12l5 5L20 7" />
                </svg>
              </div>
              <h1 className="inv-title">You&apos;re in!</h1>
              <p className="inv-subtitle">Taking you to the album...</p>
            </div>
          )}

          {phase === 'error' && (
            <div className="inv-card">
              <div className="inv-icon inv-error-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v6M12 16v.5" />
                </svg>
              </div>
              <h1 className="inv-title">Invite unavailable</h1>
              <p className="inv-subtitle">
                {errorMsg ?? 'This invite link is invalid, expired, or you no longer have access.'}
              </p>
              <Link to="/" className="inv-btn">Back to home</Link>
            </div>
          )}

          {phase === 'unauthenticated' && (
            <div className="inv-card">
              <div className="inv-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                  <path d="M16 11V7a4 4 0 00-8 0v4" />
                  <rect x="5" y="11" width="14" height="10" rx="2" />
                </svg>
              </div>
              <h1 className="inv-title">You&apos;ve been invited</h1>
              <p className="inv-subtitle">
                Sign in to Thykra to join this shared album and start contributing memories.
              </p>
              <Link to="/login" className="inv-btn">Sign in to join</Link>
              <div>
                <Link to="/" className="inv-btn-secondary">Maybe later</Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
