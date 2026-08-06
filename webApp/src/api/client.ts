const TOKEN_KEY = 'thykra_access_token';
const REFRESH_KEY = 'thykra_refresh_token';

export interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

export function saveTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

/**
 * Global 401 handling — one response interceptor, not per-screen logic.
 * Clears the JWT and redirects to /login carrying the return path.
 */
function handleSessionExpired(): never {
  clearTokens();
  const next = encodeURIComponent(
    window.location.pathname + window.location.search,
  );
  window.location.href = `/login?next=${next}`;
  throw new Error('Session expired');
}

export async function apiClient<T = unknown>(
  path: string,
  options: RequestInit = {},
): Promise<ApiEnvelope<T>> {
  const token = getAccessToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const method = options.method || 'GET';
  let response = await fetch(path, { ...options, headers });
  if (import.meta.env.DEV) console.log(`[API] ${method} ${path} → ${response.status}`);

  if (response.status === 401 && token) {
    if (getRefreshToken() && (await tryRefreshToken())) {
      headers['Authorization'] = `Bearer ${getAccessToken()}`;
      response = await fetch(path, { ...options, headers });
      if (import.meta.env.DEV) {
        console.log(`[API] ${method} ${path} → ${response.status} (after refresh)`);
      }
      if (response.status === 401) handleSessionExpired();
    } else {
      handleSessionExpired();
    }
  }

  return response.json() as Promise<ApiEnvelope<T>>;
}

/** Same client without the redirect-on-401 — for optional-auth surfaces. */
export async function apiClientPublic<T = unknown>(
  path: string,
  options: RequestInit = {},
): Promise<ApiEnvelope<T>> {
  const token = getAccessToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const response = await fetch(path, { ...options, headers });
  if (import.meta.env.DEV) {
    console.log(`[API] ${options.method || 'GET'} ${path} → ${response.status}`);
  }
  return response.json() as Promise<ApiEnvelope<T>>;
}

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Deduped: the server rotates refresh tokens, so two concurrent refresh
 * requests would invalidate each other — every caller that hits a 401 in the
 * same window must share one refresh round-trip.
 */
export function tryRefreshToken(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = doRefreshToken().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

async function doRefreshToken(): Promise<boolean> {
  try {
    const refreshToken = getRefreshToken();
    if (!refreshToken) return false;

    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) return false;

    const data = await response.json();
    if (data.success && data.data) {
      saveTokens(data.data.accessToken, data.data.refreshToken);
      return true;
    }
    return false;
  } catch {
    return false;
  }
}
