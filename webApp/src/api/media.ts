import { apiClient } from './client';

export interface MediaDto {
  id: string;
  albumId: string;
  uploaderId: string;
  type: 'PHOTO' | 'VIDEO';
  status: 'PENDING' | 'ACTIVE';
  storageKey: string;
  url: string;
  thumbnailUrl?: string;
  filename: string;
  contentType: string;
  fileSize: number;
  width?: number;
  height?: number;
  durationMs?: number;
  takenAt?: string;
  uploadedAt: string;
}

export interface RequestUploadUrlRequest {
  filename: string;
  contentType: string;
  fileSize: number;
}

export interface PresignedUploadDto {
  mediaId: string;
  storageKey: string;
  uploadUrl: string;
  method: string;
  headers: Record<string, string>;
  expiresIn: number;
}

export interface ConfirmUploadRequest {
  width?: number;
  height?: number;
  durationMs?: number;
  takenAt?: string;
}

export async function getAlbumMedia(albumId: string) {
  return apiClient(`/api/albums/${albumId}/media`);
}

export async function getMedia(albumId: string, mediaId: string) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}`);
}

export async function deleteMedia(albumId: string, mediaId: string) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}`, { method: 'DELETE' });
}

export async function requestUploadUrl(albumId: string, req: RequestUploadUrlRequest) {
  return apiClient(`/api/albums/${albumId}/media/request-upload`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function uploadFile(url: string, file: File, contentType: string): Promise<Response> {
  return fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file,
  });
}

export async function confirmUpload(albumId: string, mediaId: string, req: ConfirmUploadRequest) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/confirm`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}
