import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { getAccessToken, saveTokens, clearTokens, tryRefreshToken } from '../api/client';

interface User {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  login: (accessToken: string, refreshToken: string, user: User) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      setIsLoading(false);
      return;
    }
    const bootstrap = async () => {
      let res = await fetch('/api/profile', {
        headers: { Authorization: `Bearer ${token}` },
      });
      // An expired access token is refreshable — only give the session up
      // when the refresh itself is rejected. Network failures keep the
      // tokens: the next navigation retries.
      if (res.status === 401 && (await tryRefreshToken())) {
        res = await fetch('/api/profile', {
          headers: { Authorization: `Bearer ${getAccessToken()}` },
        });
      }
      const data = await res.json();
      if (data.success && data.data) {
        setUser(data.data);
      } else if (res.status === 401) {
        clearTokens();
      }
    };
    bootstrap()
      .catch(() => {})
      .finally(() => setIsLoading(false));
  }, []);

  const login = (accessToken: string, refreshToken: string, userData: User) => {
    saveTokens(accessToken, refreshToken);
    setUser(userData);
  };

  const logout = () => {
    const token = getAccessToken();
    if (token) {
      fetch('/api/auth/logout', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {});
    }
    clearTokens();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: user !== null,
        isLoading,
        user,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
