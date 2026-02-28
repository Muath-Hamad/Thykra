import { useState, useMemo } from 'react';
import { GoogleLogin } from '@react-oauth/google';
import { useNavigate } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';

const loginImages = [
  // Friends looking out at mountain vista together
  'https://images.unsplash.com/photo-1539635278303-d4002c07eae3?w=1920&q=85',
  // Group of friends at golden-hour beach
  'https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=1920&q=85',
  // Friends road-tripping through warm landscape
  'https://images.unsplash.com/photo-1530789253388-582c481c54b0?w=1920&q=85',
  // Traveler arms wide at sunlit mountain overlook
  'https://images.unsplash.com/photo-1504150558240-0b4fd8946624?w=1920&q=85',
  // Warm sunset over golden road — wanderlust
  'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1920&q=85',
  // Friends silhouetted against sunset sky
  'https://images.unsplash.com/photo-1473625247510-8ceb1760943f?w=1920&q=85',
  // Warm Tuscan countryside — golden hour
  'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=1920&q=85',
  // Group of friends walking together on adventure
  'https://images.unsplash.com/photo-1501555088652-021faa106b9b?w=1920&q=85',
];

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const bgImage = useMemo(
    () => loginImages[Math.floor(Math.random() * loginImages.length)],
    []
  );

  const handleGoogleSuccess = async (credentialResponse: any) => {
    const idToken = credentialResponse.credential;
    if (!idToken) return;

    try {
      setError(null);
      setLoading(true);
      const response = await fetch('/api/auth/oauth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider: 'GOOGLE', idToken }),
      });
      const data = await response.json();
      if (data.success && data.data) {
        auth.login(data.data.accessToken, data.data.refreshToken, data.data.user);
        navigate({ to: '/' });
      } else {
        setError('Authentication failed. Please try again.');
        setLoading(false);
      }
    } catch (err) {
      console.error('Login failed:', err);
      setError('Connection error. Please try again.');
      setLoading(false);
    }
  };

  return (
    <>
      <style>{`
        .login-page {
          display: grid;
          grid-template-columns: 1.1fr 0.9fr;
          min-height: 100vh;
          background: var(--color-warm-white);
          color: var(--color-deep-navy);
          font-family: var(--font-body);
          position: relative;
        }

        /* ─── LEFT — IMAGE SIDE ─── */
        .login-image-side {
          position: relative;
          overflow: hidden;
        }

        .login-bg-img {
          position: absolute;
          inset: 0;
          width: 100%;
          height: 100%;
          object-fit: cover;
          filter: brightness(0.95) contrast(1.05) saturate(1.1);
          animation: loginImgReveal 1.8s ease both;
        }

        .login-image-overlay {
          position: absolute;
          inset: 0;
          background:
            linear-gradient(180deg, rgba(253,248,239,0.1) 0%, transparent 40%, rgba(26,26,46,0.45) 100%),
            linear-gradient(90deg, transparent 70%, rgba(253,248,239,0.85) 100%);
          z-index: 1;
        }

        .login-image-content {
          position: relative;
          z-index: 2;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          height: 100%;
          padding: 3rem 4rem;
        }

        .login-img-logo {
          font-family: var(--font-display);
          font-size: 1.3rem;
          font-weight: 800;
          letter-spacing: 0.02em;
          color: #fff;
          text-shadow: 0 1px 8px rgba(0,0,0,0.3);
          animation: loginFadeUp 1s ease 0.3s both;
        }

        .login-img-bottom {
          animation: loginFadeUp 1s ease 0.6s both;
        }

        .login-image-quote {
          font-family: var(--font-display);
          font-size: 2.2rem;
          font-weight: 700;
          font-style: italic;
          line-height: 1.4;
          max-width: 420px;
          color: #fff;
          text-shadow: 0 2px 12px rgba(0,0,0,0.3);
        }

        .login-image-credit {
          font-size: 0.7rem;
          letter-spacing: 0.15em;
          text-transform: uppercase;
          color: rgba(255,255,255,0.6);
          margin-top: 1.5rem;
        }

        .login-accent-corner {
          position: absolute;
          top: 3rem;
          left: 3rem;
          width: 50px;
          height: 50px;
          border-left: 2px solid rgba(27,127,204,0.35);
          border-top: 2px solid rgba(27,127,204,0.35);
          z-index: 3;
          animation: loginFadeIn 1.5s ease 0.5s both;
        }

        .login-accent-corner-br {
          position: absolute;
          bottom: 3rem;
          right: 3rem;
          width: 50px;
          height: 50px;
          border-right: 2px solid rgba(27,127,204,0.35);
          border-bottom: 2px solid rgba(27,127,204,0.35);
          z-index: 3;
          animation: loginFadeIn 1.5s ease 0.7s both;
        }

        /* ─── RIGHT — FORM SIDE ─── */
        .login-form-side {
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
          padding: 4rem;
          position: relative;
          z-index: 2;
          background: var(--color-warm-white);
        }

        .login-back {
          position: absolute;
          top: 2.5rem;
          left: 2.5rem;
          display: flex;
          align-items: center;
          gap: 0.6rem;
          font-size: 0.8rem;
          font-weight: 500;
          color: var(--color-muted-slate);
          cursor: pointer;
          background: none;
          border: none;
          font-family: var(--font-body);
          transition: color 0.3s;
          z-index: 3;
        }
        .login-back:hover { color: var(--color-sky-blue); }
        .login-back svg { transition: transform 0.3s; }
        .login-back:hover svg { transform: translateX(-3px); }

        .login-form-inner {
          width: 100%;
          max-width: 400px;
          animation: loginFadeUp 1s ease 0.3s both;
        }

        .login-tagline {
          font-size: 0.75rem;
          letter-spacing: 0.2em;
          text-transform: uppercase;
          color: var(--color-sky-blue);
          margin-bottom: 1.5rem;
          font-weight: 600;
          font-family: var(--font-body);
        }

        .login-heading {
          font-family: var(--font-display);
          font-size: 2.8rem;
          font-weight: 900;
          margin: 0 0 0.8rem;
          line-height: 1.1;
          color: var(--color-deep-navy);
        }

        .login-heading em {
          font-style: italic;
          font-weight: 400;
          color: var(--color-sky-blue);
        }

        .login-subtext {
          font-size: 0.95rem;
          color: var(--color-muted-slate);
          line-height: 1.7;
          margin-bottom: 3rem;
          font-weight: 400;
        }

        /* ─── SIGN-IN CARD ─── */
        .login-signin-card {
          background: var(--color-sandy);
          border: 1px solid rgba(27,127,204,0.12);
          border-radius: var(--radius-md);
          padding: 2.5rem 2rem;
          position: relative;
          transition: border-color 0.4s;
          box-shadow: var(--shadow-md);
        }

        .login-signin-card:hover {
          border-color: rgba(27,127,204,0.25);
        }

        .login-card-header {
          display: flex;
          align-items: center;
          gap: 0.8rem;
          margin-bottom: 1.8rem;
        }

        .login-card-icon {
          width: 36px;
          height: 36px;
          border-radius: var(--radius-sm);
          background: rgba(27,127,204,0.08);
          border: 1px solid rgba(27,127,204,0.15);
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .login-card-title {
          font-size: 0.9rem;
          font-weight: 600;
          color: var(--color-deep-navy);
        }

        .login-card-subtitle {
          font-size: 0.75rem;
          color: var(--color-muted-slate);
          margin-top: 0.15rem;
        }

        /* Google button area */
        .login-google-area {
          background: rgba(253,248,239,0.6);
          border: 1px solid rgba(27,127,204,0.1);
          border-radius: var(--radius-sm);
          padding: 1.5rem;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 1rem;
          transition: all 0.3s;
          position: relative;
          z-index: 10;
        }

        .login-google-area:hover {
          background: rgba(253,248,239,0.9);
          border-color: rgba(27,127,204,0.2);
        }

        .login-google-hint {
          font-size: 0.72rem;
          color: var(--color-muted-slate);
          letter-spacing: 0.03em;
        }

        /* Loading overlay */
        .login-loading-overlay {
          position: absolute;
          inset: 0;
          background: rgba(253,248,239,0.9);
          border-radius: var(--radius-sm);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 1rem;
          z-index: 5;
          animation: loginFadeIn 0.3s ease both;
        }

        .login-spinner {
          width: 28px;
          height: 28px;
          border: 2px solid rgba(27,127,204,0.15);
          border-top-color: var(--color-sky-blue);
          border-radius: 50%;
          animation: loginSpin 0.8s linear infinite;
        }

        .login-loading-text {
          font-size: 0.75rem;
          color: var(--color-sky-blue);
          letter-spacing: 0.1em;
          text-transform: uppercase;
          font-weight: 600;
        }

        /* Security badge */
        .login-security {
          display: flex;
          align-items: center;
          gap: 0.6rem;
          margin-top: 1.5rem;
          padding-top: 1.5rem;
          border-top: 1px solid rgba(27,127,204,0.08);
        }

        .login-security svg {
          flex-shrink: 0;
          color: rgba(27,127,204,0.4);
        }

        .login-security-text {
          font-size: 0.7rem;
          color: var(--color-muted-slate);
          line-height: 1.5;
        }

        /* Error */
        .login-error {
          background: rgba(224,82,82,0.06);
          border: 1px solid rgba(224,82,82,0.2);
          color: var(--color-soft-red);
          padding: 0.8rem 1rem;
          border-radius: var(--radius-sm);
          font-size: 0.8rem;
          margin-bottom: 1.5rem;
          display: flex;
          align-items: center;
          gap: 0.7rem;
        }

        .login-error svg { flex-shrink: 0; }

        /* Footer */
        .login-footer-text {
          text-align: center;
          font-size: 0.72rem;
          color: var(--color-muted-slate);
          margin-top: 2.5rem;
          line-height: 1.8;
        }

        .login-footer-text a {
          color: var(--color-sky-blue);
          text-decoration: none;
          font-weight: 500;
          transition: opacity 0.3s;
        }
        .login-footer-text a:hover { opacity: 0.7; }

        /* Bottom bar */
        .login-bottom-bar {
          position: absolute;
          bottom: 2.5rem;
          left: 2.5rem;
          right: 2.5rem;
          display: flex;
          justify-content: space-between;
          align-items: center;
          font-size: 0.65rem;
          color: var(--color-muted-slate);
          letter-spacing: 0.1em;
          opacity: 0.6;
        }

        @keyframes loginFadeUp {
          from { opacity: 0; transform: translateY(25px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes loginFadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }

        @keyframes loginImgReveal {
          from { opacity: 0; transform: scale(1.08); }
          to { opacity: 1; transform: scale(1); }
        }

        @keyframes loginSpin {
          to { transform: rotate(360deg); }
        }

        @media (max-width: 900px) {
          .login-page { grid-template-columns: 1fr; }
          .login-image-side { display: none; }
          .login-form-side { padding: 3rem 2rem; }
          .login-form-inner { max-width: 100%; }
        }
      `}</style>

      <div className="login-page">
        {/* LEFT — CINEMATIC IMAGE */}
        <div className="login-image-side">
          <img className="login-bg-img" src={bgImage} alt="" />
          <div className="login-image-overlay" />
          <div className="login-accent-corner" />
          <div className="login-accent-corner-br" />
          <div className="login-image-content">
            <div className="login-img-logo">Thykra</div>
            <div className="login-img-bottom">
              <div className="login-image-quote">
                &ldquo;The best photos from a trip are never on just one phone.&rdquo;
              </div>
              <div className="login-image-credit">Thykra &mdash; Shared Memories</div>
            </div>
          </div>
        </div>

        {/* RIGHT — LOGIN FORM */}
        <div className="login-form-side">
          <button className="login-back" onClick={() => navigate({ to: '/1' })}>
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M13 8H3M7 4l-4 4 4 4" />
            </svg>
            Back
          </button>

          <div className="login-form-inner">
            <div className="login-tagline">Travel Together. Remember Forever.</div>

            <h1 className="login-heading">
              Welcome<br />
              <em>Back</em>
            </h1>
            <p className="login-subtext">
              Sign in to access your shared albums and continue capturing moments with friends.
            </p>

            {error && (
              <div className="login-error">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <circle cx="8" cy="8" r="6.5" />
                  <path d="M8 5v3.5M8 10.5v.5" />
                </svg>
                {error}
              </div>
            )}

            {/* SIGN-IN CARD */}
            <div className="login-signin-card">
              <div className="login-card-header">
                <div className="login-card-icon">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="#1B7FCC" strokeWidth="1.2">
                    <rect x="2" y="4" width="12" height="9" rx="1.5" />
                    <path d="M5 4V3a3 3 0 016 0v1" />
                    <circle cx="8" cy="9" r="1.5" />
                  </svg>
                </div>
                <div>
                  <div className="login-card-title">Secure Sign In</div>
                  <div className="login-card-subtitle">One click with your Google account</div>
                </div>
              </div>

              <div className="login-google-area">
                {loading && (
                  <div className="login-loading-overlay">
                    <div className="login-spinner" />
                    <div className="login-loading-text">Signing you in...</div>
                  </div>
                )}
                <GoogleLogin
                  onSuccess={handleGoogleSuccess}
                  onError={() => setError('Google Sign-In failed. Please try again.')}
                  theme="outline"
                  size="large"
                  shape="pill"
                  text="continue_with"
                  width={340}
                  logo_alignment="left"
                />
                <div className="login-google-hint">
                  We only access your name and email
                </div>
              </div>

              <div className="login-security">
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.2">
                  <path d="M8 1.5L2.5 4v4c0 3.5 2.3 6.2 5.5 7 3.2-.8 5.5-3.5 5.5-7V4L8 1.5z" />
                  <path d="M6 8l1.5 1.5L10 6.5" />
                </svg>
                <div className="login-security-text">
                  End-to-end secure authentication via Google OAuth 2.0. Your password is never shared with Thykra.
                </div>
              </div>
            </div>

            <div className="login-footer-text">
              By continuing, you agree to our{' '}
              <a href="#">Terms</a> &amp; <a href="#">Privacy Policy</a>
            </div>
          </div>

          <div className="login-bottom-bar">
            <span>&copy; 2024 Thykra</span>
            <span>v1.0</span>
          </div>
        </div>
      </div>
    </>
  );
}
