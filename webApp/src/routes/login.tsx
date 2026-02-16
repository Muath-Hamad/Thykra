import { GoogleLogin } from '@react-oauth/google';
import { useNavigate } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';
import { saveTokens } from '../api/client';

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();

  const handleGoogleSuccess = async (credentialResponse: any) => {
    const idToken = credentialResponse.credential;
    if (!idToken) return;

    try {
      const response = await fetch('/api/auth/oauth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider: 'GOOGLE', idToken }),
      });
      const data = await response.json();
      if (data.success && data.data) {
        auth.login(data.data.accessToken, data.data.refreshToken, data.data.user);
        navigate({ to: '/' });
      }
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', padding: '2rem' }}>
      <h1 style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>Thykra</h1>
      <p style={{ color: '#666', marginBottom: '3rem' }}>Travel Together. Remember Forever.</p>
      <GoogleLogin
        onSuccess={handleGoogleSuccess}
        onError={() => console.error('Google Sign-In failed')}
      />
    </div>
  );
}
