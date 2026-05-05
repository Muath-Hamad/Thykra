import { apiClient } from './client';

export type ReactionType =
  | 'LOVE'
  | 'WOW'
  | 'LAUGH'
  | 'WISH_I_WAS_THERE'
  | 'WANDERLUST'
  | 'BEACH'
  | 'MOUNTAIN'
  | 'FOOD';

export const REACTION_TYPES: ReactionType[] = [
  'LOVE',
  'WOW',
  'LAUGH',
  'WISH_I_WAS_THERE',
  'WANDERLUST',
  'BEACH',
  'MOUNTAIN',
  'FOOD',
];

export const REACTION_EMOJI: Record<ReactionType, string> = {
  LOVE: '❤️',
  WOW: '😮',
  LAUGH: '😂',
  WISH_I_WAS_THERE: '✈️',
  WANDERLUST: '🌍',
  BEACH: '🏖️',
  MOUNTAIN: '⛰️',
  FOOD: '🍜',
};

export const REACTION_LABEL: Record<ReactionType, string> = {
  LOVE: 'Love',
  WOW: 'Wow',
  LAUGH: 'Laugh',
  WISH_I_WAS_THERE: 'Wish I was there',
  WANDERLUST: 'Wanderlust',
  BEACH: 'Beach',
  MOUNTAIN: 'Mountain',
  FOOD: 'Food',
};

export interface ReactionDto {
  id: string;
  mediaId: string;
  userId: string;
  userDisplayName: string;
  userAvatarUrl?: string;
  type: ReactionType;
  createdAt: string;
}

export interface ReactionSummaryDto {
  type: ReactionType;
  count: number;
  reactedByMe: boolean;
}

export interface MediaReactionsDto {
  summary: ReactionSummaryDto[];
  reactions: ReactionDto[];
}

export async function getReactions(albumId: string, mediaId: string) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/reactions`);
}

export async function toggleReaction(albumId: string, mediaId: string, type: ReactionType) {
  return apiClient(`/api/albums/${albumId}/media/${mediaId}/reactions`, {
    method: 'POST',
    body: JSON.stringify({ type }),
  });
}
