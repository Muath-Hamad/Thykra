import { apiClient } from './client';

export type AlbumVisibility = 'PRIVATE' | 'LINK_SHARED';

export interface AlbumMemberSummary {
  userId: string;
  displayName: string;
  avatarUrl?: string;
}

export interface AlbumDto {
  id: string;
  ownerId: string;
  title: string;
  description?: string;
  coverUrl?: string;
  visibility: AlbumVisibility;
  memberCount: number;
  previewMembers: AlbumMemberSummary[];
  createdAt: string;
}

export interface AlbumMemberDto {
  userId: string;
  displayName: string;
  avatarUrl?: string;
  role: 'OWNER' | 'CONTRIBUTOR' | 'VIEWER';
  joinedAt: string;
}

export interface InviteLinkDto {
  albumId: string;
  token: string;
  expiresAt: string;
  /** How many people joined with this link — server addition. */
  joinCount?: number;
}

export interface BlockedMemberDto {
  userId: string;
  displayName: string;
  avatarUrl?: string;
  blockedAt: string;
}

export interface UpdateAlbumRequest {
  title?: string;
  description?: string;
  coverUrl?: string;
  visibility?: AlbumVisibility;
}

export async function getAlbums() {
  return apiClient<AlbumDto[]>('/api/albums');
}

export async function getAlbum(id: string) {
  return apiClient<AlbumDto>(`/api/albums/${id}`);
}

export async function createAlbum(title: string, description?: string) {
  return apiClient<AlbumDto>('/api/albums', {
    method: 'POST',
    body: JSON.stringify({ title, description }),
  });
}

export async function updateAlbum(id: string, data: UpdateAlbumRequest) {
  return apiClient<AlbumDto>(`/api/albums/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteAlbum(id: string) {
  return apiClient<string>(`/api/albums/${id}`, { method: 'DELETE' });
}

export async function getMembers(albumId: string) {
  return apiClient<AlbumMemberDto[]>(`/api/albums/${albumId}/members`);
}

export async function addMember(albumId: string, userId: string, role: string) {
  return apiClient<AlbumMemberDto>(`/api/albums/${albumId}/members`, {
    method: 'POST',
    body: JSON.stringify({ userId, role }),
  });
}

export async function removeMember(albumId: string, userId: string) {
  return apiClient<string>(`/api/albums/${albumId}/members/${userId}`, { method: 'DELETE' });
}

export async function createInviteLink(albumId: string, expiresInDays?: number) {
  return apiClient<InviteLinkDto>(`/api/albums/${albumId}/invite`, {
    method: 'POST',
    body: JSON.stringify({ expiresInDays }),
  });
}

export async function joinByInvite(token: string) {
  return apiClient<AlbumDto>(`/api/albums/join/${token}`, { method: 'POST' });
}

export async function getBlockedMembers(albumId: string) {
  return apiClient<BlockedMemberDto[]>(`/api/albums/${albumId}/blocks`);
}

export async function blockMember(albumId: string, userId: string) {
  return apiClient<string>(`/api/albums/${albumId}/blocks/${userId}`, { method: 'POST' });
}

export async function unblockMember(albumId: string, userId: string) {
  return apiClient<string>(`/api/albums/${albumId}/blocks/${userId}`, { method: 'DELETE' });
}
