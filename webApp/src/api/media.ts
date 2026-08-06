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
  return apiClient<MediaDto[]>(`/api/albums/${albumId}/media`);
}

export async function getMedia(albumId: string, mediaId: string) {
  return apiClient<MediaDto>(`/api/albums/${albumId}/media/${mediaId}`);
}

export async function deleteMedia(albumId: string, mediaId: string) {
  return apiClient<string>(`/api/albums/${albumId}/media/${mediaId}`, { method: 'DELETE' });
}

export async function requestUploadUrl(albumId: string, req: RequestUploadUrlRequest) {
  return apiClient<PresignedUploadDto>(`/api/albums/${albumId}/media/request-upload`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Presigned PUT via XMLHttpRequest — xhr.upload.onprogress gives true bytes
 * with no API change (Doc 2, "Honest progress"). This is what makes the
 * per-file upload bars real rather than decorative.
 */
export function uploadFileWithProgress(
  url: string,
  file: File,
  contentType: string,
  onProgress: (loaded: number, total: number) => void,
  signal?: AbortSignal,
): Promise<{ ok: boolean; status: number }> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', url);
    xhr.setRequestHeader('Content-Type', contentType);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(e.loaded, e.total);
    };
    xhr.onload = () => {
      if (import.meta.env.DEV) console.log(`[API] PUT ${url} → ${xhr.status}`);
      resolve({ ok: xhr.status >= 200 && xhr.status < 300, status: xhr.status });
    };
    xhr.onerror = () => reject(new Error('Network error during upload'));
    xhr.onabort = () => reject(new DOMException('Aborted', 'AbortError'));
    if (signal) {
      if (signal.aborted) {
        xhr.abort();
        return;
      }
      signal.addEventListener('abort', () => xhr.abort(), { once: true });
    }
    xhr.send(file);
  });
}

export async function confirmUpload(albumId: string, mediaId: string, req: ConfirmUploadRequest) {
  return apiClient<MediaDto>(`/api/albums/${albumId}/media/${mediaId}/confirm`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}
