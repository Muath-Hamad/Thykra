// Public album API — does NOT require authentication.
// Used by the unauthenticated /public/:id share route.

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

export interface PublicMediaDto {
  id: string;
  type: 'PHOTO' | 'VIDEO';
  url: string;
  thumbnailUrl?: string;
  width?: number;
  height?: number;
  takenAt?: string;
  uploadedAt: string;
}

export interface PublicAlbumViewDto {
  album: PublicAlbumDto;
  media: PublicMediaDto[];
}

interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export async function getPublicAlbum(id: string): Promise<ApiEnvelope<PublicAlbumViewDto>> {
  const resp = await fetch(`/api/public/albums/${id}`);
  if (import.meta.env.DEV) console.log(`[API] GET /api/public/albums/${id} -> ${resp.status}`);
  return resp.json() as Promise<ApiEnvelope<PublicAlbumViewDto>>;
}
