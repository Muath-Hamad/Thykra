import { apiClient } from './client';

export interface AlbumDto {
  id: string;
  ownerId: string;
  title: string;
  description?: string;
  coverUrl?: string;
  memberCount: number;
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
}

export async function getAlbums() {
  return apiClient('/api/albums');
}

export async function getAlbum(id: string) {
  return apiClient(`/api/albums/${id}`);
}

export async function createAlbum(title: string, description?: string) {
  return apiClient('/api/albums', {
    method: 'POST',
    body: JSON.stringify({ title, description }),
  });
}

export async function updateAlbum(id: string, data: { title?: string; description?: string; coverUrl?: string }) {
  return apiClient(`/api/albums/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteAlbum(id: string) {
  return apiClient(`/api/albums/${id}`, { method: 'DELETE' });
}

export async function getMembers(albumId: string) {
  return apiClient(`/api/albums/${albumId}/members`);
}

export async function addMember(albumId: string, userId: string, role: string) {
  return apiClient(`/api/albums/${albumId}/members`, {
    method: 'POST',
    body: JSON.stringify({ userId, role }),
  });
}

export async function removeMember(albumId: string, userId: string) {
  return apiClient(`/api/albums/${albumId}/members/${userId}`, { method: 'DELETE' });
}

export async function createInviteLink(albumId: string) {
  return apiClient(`/api/albums/${albumId}/invite`, { method: 'POST' });
}

export async function joinByInvite(token: string) {
  return apiClient(`/api/albums/join/${token}`, { method: 'POST' });
}
