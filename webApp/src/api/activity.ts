import { apiClient, type ApiEnvelope } from './client';
import type { ReactionType } from './reactions';

export type ActivityEventType = 'MEDIA_ADDED' | 'MEMBER_JOINED' | 'COMMENT' | 'REACTION';

export interface ActivityEventDto {
  id: string;
  type: ActivityEventType;
  actor: { userId: string; displayName: string; avatarUrl?: string | null };
  album: { id: string; title: string };
  createdAt: string;
  count?: number | null;
  mediaIds: string[];
  thumbnailUrls: string[];
  commentBody?: string | null;
  reactionType?: ReactionType | null;
}

export interface ActivityFeedDto {
  items: ActivityEventDto[];
  nextCursor?: string | null;
  lastSeenAt?: string | null;
}

export async function getGlobalActivity(
  cursor?: string,
  limit = 30,
): Promise<ApiEnvelope<ActivityFeedDto>> {
  const params = new URLSearchParams();
  if (cursor) params.set('cursor', cursor);
  params.set('limit', String(limit));
  return apiClient<ActivityFeedDto>(`/api/activity?${params}`);
}

export async function getAlbumActivity(
  albumId: string,
  cursor?: string,
  limit = 30,
): Promise<ApiEnvelope<ActivityFeedDto>> {
  const params = new URLSearchParams();
  if (cursor) params.set('cursor', cursor);
  params.set('limit', String(limit));
  return apiClient<ActivityFeedDto>(`/api/albums/${albumId}/activity?${params}`);
}

export async function markActivitySeen(seenAt?: string): Promise<ApiEnvelope<string>> {
  return apiClient<string>('/api/activity/seen', {
    method: 'POST',
    body: JSON.stringify({ seenAt }),
  });
}
