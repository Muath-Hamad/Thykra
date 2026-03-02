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
  console.log('[Media API] requestUploadUrl:', { albumId, req });
  const resp = await apiClient(`/api/albums/${albumId}/media/request-upload`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
  console.log('[Media API] requestUploadUrl response:', resp);
  return resp;
}

export async function uploadFile(url: string, file: File, contentType: string): Promise<Response> {
  console.log('[Media API] uploadFile:', { url, fileName: file.name, contentType, size: file.size });
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': contentType },
      body: file,
    });
    console.log('[Media API] uploadFile response:', { status: resp.status, statusText: resp.statusText });
    return resp;
  } catch (err) {
    console.error('[Media API] uploadFile FAILED:', { url, error: err });
    throw err;
  }
}

export async function confirmUpload(albumId: string, mediaId: string, req: ConfirmUploadRequest) {
  console.log('[Media API] confirmUpload:', { albumId, mediaId, req });
  const resp = await apiClient(`/api/albums/${albumId}/media/${mediaId}/confirm`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
  console.log('[Media API] confirmUpload response:', resp);
  return resp;
}
