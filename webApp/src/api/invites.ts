import { apiClient, apiClientPublic, type ApiEnvelope } from './client';
import type { AlbumDto } from './albums';

export type InviteStatus = 'VALID' | 'EXPIRED' | 'REVOKED' | 'ALREADY_MEMBER' | 'BLOCKED';

export interface InvitePreviewDto {
  album?: AlbumDto | null;
  invitedBy?: { displayName: string; avatarUrl?: string | null } | null;
  mediaCount?: number | null;
  expiresAt?: string | null;
  status: InviteStatus;
  previewMedia: { thumbnailUrl: string }[];
  /** Present on ALREADY_MEMBER — when the caller joined. */
  joinedAt?: string | null;
}

export interface InviteLinkDto {
  albumId: string;
  token: string;
  expiresAt: string;
  joinCount: number;
}

/** Unauthenticated (auth-aware) invite preview — the J2 endpoint. */
export async function getInvitePreview(token: string): Promise<ApiEnvelope<InvitePreviewDto>> {
  return apiClientPublic<InvitePreviewDto>(`/api/invites/${encodeURIComponent(token)}/preview`);
}

export async function listInvites(albumId: string): Promise<ApiEnvelope<InviteLinkDto[]>> {
  return apiClient<InviteLinkDto[]>(`/api/albums/${albumId}/invites`);
}

export async function revokeInvite(albumId: string, token: string): Promise<ApiEnvelope<string>> {
  return apiClient<string>(`/api/albums/${albumId}/invites/${encodeURIComponent(token)}`, {
    method: 'DELETE',
  });
}
