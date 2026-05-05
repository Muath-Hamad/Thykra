import { apiClient } from './client';

export interface CommentDto {
  id: string;
  mediaId: string;
  authorId: string;
  authorDisplayName: string;
  authorAvatarUrl?: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export const MAX_COMMENT_LENGTH = 2000;

export async function getComments(albumId: string, mediaId: string) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/comments`);
}

export async function addComment(albumId: string, mediaId: string, body: string) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
}

export async function updateComment(
  albumId: string,
  mediaId: string,
  commentId: string,
  body: string,
) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/comments/${commentId}`, {
    method: 'PUT',
    body: JSON.stringify({ body }),
  });
}

export async function deleteComment(albumId: string, mediaId: string, commentId: string) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/comments/${commentId}`, {
    method: 'DELETE',
  });
}
