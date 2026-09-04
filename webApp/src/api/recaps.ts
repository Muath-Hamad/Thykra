import { apiClient, apiClientPublic, type ApiEnvelope } from './client';
import type { AlbumMemberSummary } from './albums';
import type { PublicMediaDto } from './publicAlbums';

export type RecapStatus = 'BUILDING' | 'READY' | 'FAILED';

export interface RecapDto {
  id: string;
  albumId: string;
  albumTitle: string;
  title: string;
  coverMediaId?: string | null;
  coverUrl?: string | null;
  mediaIds: string[];
  periodStart?: string | null;
  periodEnd?: string | null;
  status: RecapStatus;
  shareToken?: string | null;
  createdAt: string;
}

export interface RecapViewDto {
  recap: RecapDto;
  media: PublicMediaDto[];
  ownerDisplayName: string;
  contributors: AlbumMemberSummary[];
}

/** "NOT_ENOUGH_MEDIA:{needed}" — the honest gate. */
export function parseRecapGate(error?: string): number | null {
  const match = error?.match(/^NOT_ENOUGH_MEDIA:(\d+)$/);
  return match ? Number(match[1]) : null;
}

export async function getMyRecaps(): Promise<ApiEnvelope<RecapDto[]>> {
  return apiClient<RecapDto[]>('/api/recaps');
}

export async function getAlbumRecaps(albumId: string): Promise<ApiEnvelope<RecapDto[]>> {
  return apiClient<RecapDto[]>(`/api/albums/${albumId}/recaps`);
}

export async function requestRecap(albumId: string): Promise<ApiEnvelope<RecapDto>> {
  return apiClient<RecapDto>(`/api/albums/${albumId}/recaps`, { method: 'POST' });
}

export async function getPublicRecap(shareToken: string): Promise<ApiEnvelope<RecapViewDto>> {
  return apiClientPublic<RecapViewDto>(`/api/r/${encodeURIComponent(shareToken)}`);
}
