/**
 * Per-trip last-visit stamps in localStorage — powers the "12 new" badge
 * and the "Sara added 12 photos · 2h ago" line on TripCard, plus the
 * once-per-trip welcome banner. No API involved (Doc 2 TripCard notes).
 */

const VISIT_PREFIX = 'thykra.lastVisit.';
const WELCOME_PREFIX = 'thykra.welcomed.';
const ACTIVITY_SEEN_KEY = 'thykra.activitySeen';

export function getLastVisit(albumId: string): string | null {
  try {
    return localStorage.getItem(VISIT_PREFIX + albumId);
  } catch {
    return null;
  }
}

export function markVisited(albumId: string, when: Date = new Date()) {
  try {
    localStorage.setItem(VISIT_PREFIX + albumId, when.toISOString());
  } catch {
    /* private mode */
  }
}

/** Count of media uploaded since the viewer's last visit to the trip. */
export function countNewSince(
  media: { uploadedAt: string; uploaderId: string }[],
  lastVisitIso: string | null,
  viewerId?: string,
): number {
  if (!lastVisitIso) return 0;
  const cutoff = Date.parse(lastVisitIso);
  return media.filter(
    (m) => Date.parse(m.uploadedAt) > cutoff && m.uploaderId !== viewerId,
  ).length;
}

export function wasWelcomed(albumId: string): boolean {
  try {
    return localStorage.getItem(WELCOME_PREFIX + albumId) === '1';
  } catch {
    return true; // fail closed: never nag when storage is unavailable
  }
}

export function markWelcomed(albumId: string) {
  try {
    localStorage.setItem(WELCOME_PREFIX + albumId, '1');
  } catch {
    /* private mode */
  }
}

export function getActivitySeen(): string | null {
  try {
    return localStorage.getItem(ACTIVITY_SEEN_KEY);
  } catch {
    return null;
  }
}

export function markActivitySeen(when: Date = new Date()) {
  try {
    localStorage.setItem(ACTIVITY_SEEN_KEY, when.toISOString());
  } catch {
    /* private mode */
  }
}
