import { describe, expect, it } from 'vitest';
import {
  groupIntoChapters,
  formatOrdinal,
  chapterOf,
  localDateOf,
  type ChapterMedia,
} from './chapters';

let seq = 0;
function media(
  overrides: Partial<ChapterMedia> & { takenAt?: string | null },
): ChapterMedia {
  seq += 1;
  return {
    id: `m${seq}`,
    type: 'PHOTO',
    uploadedAt: '2026-04-14T10:00:00Z',
    ...overrides,
  } as ChapterMedia;
}

// Use local-noon timestamps so viewer-timezone grouping is deterministic
// regardless of the machine's offset.
const d = (day: number, hour = 12) =>
  new Date(2026, 3, day, hour, 0, 0).toISOString();

describe('groupIntoChapters — the five exact rules', () => {
  it('rule 1: sorts by takenAt ?? uploadedAt ascending and groups by local date', () => {
    const items = [
      media({ takenAt: d(12, 17) }),
      media({ takenAt: d(11, 9) }),
      media({ takenAt: d(11, 20) }),
    ];
    const chapters = groupIntoChapters(items);
    expect(chapters).toHaveLength(2);
    expect(chapters[0].items.map((m) => m.takenAt)).toEqual([d(11, 9), d(11, 20)]);
    expect(chapters[1].items[0].takenAt).toBe(d(12, 17));
  });

  it('rule 2: ordinals are ordinal within the trip — gaps do not create empty chapters', () => {
    const items = [
      media({ takenAt: d(11) }),
      media({ takenAt: d(15) }), // 3-day gap
      media({ takenAt: d(16) }),
    ];
    const chapters = groupIntoChapters(items);
    expect(chapters.map((c) => c.ordinal)).toEqual([1, 2, 3]);
  });

  it('rule 3: null takenAt groups by uploadedAt, no numeral, sorted after dated', () => {
    const items = [
      media({ takenAt: null, uploadedAt: d(10) }), // uploaded BEFORE the dated days
      media({ takenAt: d(12) }),
      media({ takenAt: d(11) }),
    ];
    const chapters = groupIntoChapters(items);
    expect(chapters).toHaveLength(3);
    // Dated chapters first, in date order, with ordinals.
    expect(chapters[0].dated).toBe(true);
    expect(chapters[0].ordinal).toBe(1);
    expect(chapters[1].ordinal).toBe(2);
    // Undated last, no ordinal, even though its date precedes the dated ones.
    expect(chapters[2].dated).toBe(false);
    expect(chapters[2].ordinal).toBeNull();
  });

  it('rule 4: single-date trip renders one chapter with the numeral suppressed', () => {
    const items = [media({ takenAt: d(11, 9) }), media({ takenAt: d(11, 15) })];
    const chapters = groupIntoChapters(items);
    expect(chapters).toHaveLength(1);
    expect(chapters[0].ordinal).toBeNull();
    expect(chapters[0].dated).toBe(true);
  });

  it('rule 4 counter-case: a single dated chapter plus an undated one keeps the numeral', () => {
    const items = [
      media({ takenAt: d(11) }),
      media({ takenAt: null, uploadedAt: d(14) }),
    ];
    const chapters = groupIntoChapters(items);
    expect(chapters[0].ordinal).toBe(1);
    expect(chapters[1].ordinal).toBeNull();
  });

  it('rule 5: the lead plate is the earliest item of the day', () => {
    const early = media({ takenAt: d(11, 6) });
    const late = media({ takenAt: d(11, 18) });
    const chapters = groupIntoChapters([late, early]);
    expect(chapters[0].lead.id).toBe(early.id);
    expect(chapters[0].rest.map((m) => m.id)).toEqual([late.id]);
  });

  it('counts photos and videos per chapter', () => {
    const items = [
      media({ takenAt: d(11) }),
      { ...media({ takenAt: d(11) }), type: 'VIDEO' as const },
    ];
    const [chapter] = groupIntoChapters(items);
    expect(chapter.photoCount).toBe(1);
    expect(chapter.videoCount).toBe(1);
  });

  it('chapterOf finds the chapter containing a media id', () => {
    const a = media({ takenAt: d(11) });
    const b = media({ takenAt: d(12) });
    const chapters = groupIntoChapters([a, b]);
    expect(chapterOf(chapters, b.id)?.ordinal).toBe(2);
    expect(chapterOf(chapters, 'nope')).toBeUndefined();
  });
});

describe('formatOrdinal', () => {
  it('pads to two digits', () => {
    expect(formatOrdinal(1)).toBe('01');
    expect(formatOrdinal(12)).toBe('12');
  });
  it('renders Arabic-Indic digits for Arabic display typography', () => {
    expect(formatOrdinal(1, true)).toBe('٠١');
    expect(formatOrdinal(23, true)).toBe('٢٣');
  });
});

describe('localDateOf', () => {
  it('returns the viewer-local calendar date', () => {
    const iso = new Date(2026, 3, 11, 23, 30).toISOString();
    expect(localDateOf(iso)).toBe('2026-04-11');
  });
});
