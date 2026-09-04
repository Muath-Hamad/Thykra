// Public album API — does NOT require authentication.
// Used by the unauthenticated /public/:id and /s/:id share routes.

import type { ApiEnvelope } from './client';
import type { ReactionType } from './reactions';

export interface PublicAlbumDto {
  id: string;
  title: string;
  description?: string;
  coverUrl?: string;
  ownerDisplayName: string;
  ownerAvatarUrl?: string;
  mediaCount: number;
  createdAt: string;
}

export interface PublicReactionSummaryDto {
  type: ReactionType;
  count: number;
}

export interface PublicMediaDto {
  id: string;
  type: 'PHOTO' | 'VIDEO';
  url: string;
  thumbnailUrl?: string;
  width?: number;
  height?: number;
  durationMs?: number;
  takenAt?: string;
  uploadedAt: string;
  /** Counts only, no identities — absent until the API ships it. */
  reactionSummary?: PublicReactionSummaryDto[];
}

export interface PublicAlbumViewDto {
  album: PublicAlbumDto;
  media: PublicMediaDto[];
}

export async function getPublicAlbum(id: string): Promise<ApiEnvelope<PublicAlbumViewDto>> {
  const resp = await fetch(`/api/public/albums/${id}`);
  if (import.meta.env.DEV) console.log(`[API] GET /api/public/albums/${id} -> ${resp.status}`);
  return resp.json() as Promise<ApiEnvelope<PublicAlbumViewDto>>;
}
