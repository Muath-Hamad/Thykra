import { describe, expect, it } from 'vitest';
import { deriveTripActivity, AGGREGATION_WINDOW_MS } from './activityDerive';
import type { MediaDto } from '../api/media';
import type { AlbumMemberDto } from '../api/albums';

let seq = 0;
function m(uploaderId: string, uploadedAt: string): MediaDto {
  seq += 1;
  return {
    id: `m${seq}`,
    albumId: 'a1',
    uploaderId,
    type: 'PHOTO',
    status: 'ACTIVE',
    storageKey: `k${seq}`,
    url: `u${seq}`,
    thumbnailUrl: `t${seq}`,
    filename: `f${seq}.jpg`,
    contentType: 'image/jpeg',
    fileSize: 1,
    uploadedAt,
  };
}

const member = (userId: string, role: AlbumMemberDto['role'], joinedAt: string): AlbumMemberDto => ({
  userId,
  displayName: `User ${userId}`,
  role,
  joinedAt,
});

const at = (minutes: number) => new Date(Date.UTC(2026, 3, 12, 10, minutes)).toISOString();

describe('deriveTripActivity', () => {
  const members = [
    member('owner', 'OWNER', at(0)),
    member('sara', 'CONTRIBUTOR', at(5)),
  ];

  it('aggregates same-uploader uploads within the 30-minute window into one event', () => {
    const media = [m('sara', at(10)), m('sara', at(20)), m('sara', at(35))];
    const events = deriveTripActivity(media, members).filter((e) => e.type === 'MEDIA_ADDED');
    expect(events).toHaveLength(1);
    expect(events[0].count).toBe(3);
    expect(events[0].mediaIds).toHaveLength(3);
    expect(events[0].actor.displayName).toBe('User sara');
  });

  it('splits uploads more than 30 minutes apart into separate events', () => {
    const gapMinutes = AGGREGATION_WINDOW_MS / 60000 + 5;
    const media = [m('sara', at(0)), m('sara', at(gapMinutes))];
    const events = deriveTripActivity(media, members).filter((e) => e.type === 'MEDIA_ADDED');
    expect(events).toHaveLength(2);
    expect(events.every((e) => e.count === 1)).toBe(true);
  });

  it('does not merge different uploaders', () => {
    const media = [m('sara', at(0)), m('owner', at(1))];
    const events = deriveTripActivity(media, members).filter((e) => e.type === 'MEDIA_ADDED');
    expect(events).toHaveLength(2);
  });

  it('emits MEMBER_JOINED for non-owners only', () => {
    const events = deriveTripActivity([], members).filter((e) => e.type === 'MEMBER_JOINED');
    expect(events).toHaveLength(1);
    expect(events[0].actor.userId).toBe('sara');
  });

  it('sorts newest first', () => {
    const media = [m('sara', at(50))];
    const events = deriveTripActivity(media, members);
    expect(events[0].type).toBe('MEDIA_ADDED');
    expect(events[1].type).toBe('MEMBER_JOINED');
  });

  it('caps thumbnails at 4', () => {
    const media = Array.from({ length: 6 }, (_, i) => m('sara', at(i)));
    const [event] = deriveTripActivity(media, members).filter((e) => e.type === 'MEDIA_ADDED');
    expect(event.thumbnailUrls).toHaveLength(4);
    expect(event.count).toBe(6);
  });
});
