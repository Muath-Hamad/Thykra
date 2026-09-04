import type { MediaDto } from '../api/media';
import type { AlbumMemberDto } from '../api/albums';

/**
 * Client-side activity derivation — ships before the feed endpoint exists
 * (Doc 3 § activity, "Ships before the endpoint"). Groups a trip's media by
 * uploader into 30-minute buckets to produce MEDIA_ADDED rows, and turns
 * joinedAt into MEMBER_JOINED. Comments and reactions cannot be derived
 * without per-media requests, so those come only from the server feed.
 */

export const AGGREGATION_WINDOW_MS = 30 * 60 * 1000;

export interface DerivedActivityEvent {
  id: string;
  type: 'MEDIA_ADDED' | 'MEMBER_JOINED';
  actor: { userId: string; displayName: string; avatarUrl?: string };
  createdAt: string;
  /** MEDIA_ADDED only */
  count?: number;
  mediaIds?: string[];
  thumbnailUrls?: string[];
}

export function deriveTripActivity(
  media: MediaDto[],
  members: AlbumMemberDto[],
): DerivedActivityEvent[] {
  const memberById = new Map(members.map((m) => [m.userId, m]));
  const events: DerivedActivityEvent[] = [];

  // MEDIA_ADDED — bucket per uploader per 30-minute window.
  const byUploader = new Map<string, MediaDto[]>();
  for (const m of media) {
    (byUploader.get(m.uploaderId) ?? byUploader.set(m.uploaderId, []).get(m.uploaderId)!).push(m);
  }
  for (const [uploaderId, items] of byUploader) {
    const sorted = [...items].sort(
      (a, b) => Date.parse(a.uploadedAt) - Date.parse(b.uploadedAt),
    );
    let bucket: MediaDto[] = [];
    let bucketStart = 0;
    const flush = () => {
      if (bucket.length === 0) return;
      const last = bucket[bucket.length - 1];
      const member = memberById.get(uploaderId);
      events.push({
        id: `added:${uploaderId}:${last.uploadedAt}`,
        type: 'MEDIA_ADDED',
        actor: {
          userId: uploaderId,
          displayName: member?.displayName ?? '—',
          avatarUrl: member?.avatarUrl,
        },
        createdAt: last.uploadedAt,
        count: bucket.length,
        mediaIds: bucket.map((b) => b.id),
        thumbnailUrls: bucket
          .slice(0, 4)
          .map((b) => b.thumbnailUrl ?? b.url),
      });
      bucket = [];
    };
    for (const m of sorted) {
      const t = Date.parse(m.uploadedAt);
      if (bucket.length === 0 || t - bucketStart <= AGGREGATION_WINDOW_MS) {
        if (bucket.length === 0) bucketStart = t;
        bucket.push(m);
      } else {
        flush();
        bucketStart = t;
        bucket.push(m);
      }
    }
    flush();
  }

  // MEMBER_JOINED — every non-owner join.
  for (const member of members) {
    if (member.role === 'OWNER') continue;
    events.push({
      id: `joined:${member.userId}`,
      type: 'MEMBER_JOINED',
      actor: {
        userId: member.userId,
        displayName: member.displayName,
        avatarUrl: member.avatarUrl,
      },
      createdAt: member.joinedAt,
    });
  }

  return events.sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
}
