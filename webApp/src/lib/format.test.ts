import { describe, expect, it } from 'vitest';
import {
  formatFileSize,
  formatDuration,
  relativeTimeParts,
  timeOfDay,
} from './format';
import { interpolate } from '../i18n/LocaleProvider';
import { tintFor, initialsOf, AVATAR_TINTS, stackLabel } from '../ui/Avatar';

describe('formatFileSize', () => {
  it('humanises byte counts', () => {
    expect(formatFileSize(512)).toBe('512 B');
    expect(formatFileSize(2048)).toBe('2 KB');
    expect(formatFileSize(4.2 * 1024 * 1024)).toBe('4.2 MB');
    expect(formatFileSize(1.5 * 1024 * 1024 * 1024)).toBe('1.5 GB');
  });
});

describe('formatDuration — m:ss badge', () => {
  it('formats durations', () => {
    expect(formatDuration(14_000)).toBe('0:14');
    expect(formatDuration(125_000)).toBe('2:05');
    expect(formatDuration(60_000)).toBe('1:00');
  });
});

describe('relativeTimeParts', () => {
  const now = new Date('2026-04-12T15:00:00Z');
  it('buckets correctly', () => {
    expect(relativeTimeParts('2026-04-12T14:59:40Z', now)).toEqual({ kind: 'justNow' });
    expect(relativeTimeParts('2026-04-12T14:20:00Z', now)).toEqual({ kind: 'minutes', n: 40 });
    expect(relativeTimeParts('2026-04-10T15:00:00Z', now)).toEqual({ kind: 'days', n: 2 });
    // Calendar days, not 24h blocks: 47 hours ago is two days ago, not yesterday.
    expect(relativeTimeParts('2026-04-11T20:00:00Z', now)).toEqual({ kind: 'yesterday' });
    expect(relativeTimeParts('2026-04-10T16:00:00Z', now)).toEqual({ kind: 'days', n: 2 });
    expect(relativeTimeParts('2026-04-01T15:00:00Z', now)).toEqual({
      kind: 'date',
      iso: '2026-04-01T15:00:00Z',
    });
  });
});

describe('timeOfDay', () => {
  it('buckets by local hour', () => {
    expect(timeOfDay(new Date(2026, 3, 12, 8))).toBe('morning');
    expect(timeOfDay(new Date(2026, 3, 12, 13))).toBe('afternoon');
    expect(timeOfDay(new Date(2026, 3, 12, 20))).toBe('evening');
  });
});

describe('i18n interpolate', () => {
  it('replaces {name} placeholders and leaves unknown ones', () => {
    expect(interpolate('Hi {name}, {n} photos', { name: 'Sara', n: 12 })).toBe(
      'Hi Sara, 12 photos',
    );
    expect(interpolate('Hi {name}', {})).toBe('Hi {name}');
  });
});

describe('avatar tints — deterministic, ink-compatible', () => {
  it('same user id always maps to the same tint', () => {
    expect(tintFor('user-abc')).toBe(tintFor('user-abc'));
    expect(AVATAR_TINTS).toContain(tintFor('user-abc'));
  });
  it('initials come from the first word', () => {
    expect(initialsOf('Muath Hamad')).toBe('M');
    expect(initialsOf('  sara  ')).toBe('S');
    expect(initialsOf('')).toBe('·');
  });
  it('stack label reads as a real sentence', () => {
    const people = [
      { userId: '1', displayName: 'Muath H' },
      { userId: '2', displayName: 'Sara N' },
      { userId: '3', displayName: 'Omar K' },
      { userId: '4', displayName: 'Lina B' },
    ];
    expect(stackLabel(people, 6)).toBe('6 people — Muath, Sara, Omar, Lina and 2 more');
  });
});
