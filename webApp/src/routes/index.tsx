import { Link } from '@tanstack/react-router';
import { useAuth } from '../auth/AuthContext';

export function HomePage() {
  const auth = useAuth();

  return (
    <div style={{ padding: '2rem' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Thykra</h1>
        <nav style={{ display: 'flex', gap: '1rem' }}>
          <Link to="/profile">Profile</Link>
          <button onClick={auth.logout}>Logout</button>
        </nav>
      </header>
      <main>
        <p>Welcome{auth.user ? `, ${auth.user.displayName}` : ''}!</p>
        <p>Your shared albums will appear here.</p>
      </main>
    </div>
  );
}
