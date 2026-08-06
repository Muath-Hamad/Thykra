/**
 * Day-chapter grouping — the exact rules from Doc 3 (§ /trips/:id):
 *
 * 1. Sort all media by `takenAt ?? uploadedAt` ascending; group by calendar
 *    date in the *viewer's* timezone.
 * 2. Day numerals are ordinal within the trip — the first day with any media
 *    is 01, regardless of gaps. A 3-day gap does not create empty chapters.
 * 3. Items with null `takenAt` group by `uploadedAt`, but their chapter reads
 *    "Added {date}" with no numeral, and those chapters sort after all dated
 *    ones.
 * 4. A trip whose media all share one date renders exactly one chapter and
 *    the numeral is suppressed.
 * 5. The lead plate is the earliest item of the day.
 */

/** Minimal structural shape — satisfied by MediaDto and PublicMediaDto. */
export interface ChapterMedia {
  id: string;
  type: 'PHOTO' | 'VIDEO';
  takenAt?: string | null;
  uploadedAt: string;
}

export interface Chapter<T extends ChapterMedia = ChapterMedia> {
  /** Stable key: `d:2026-04-12` for dated, `u:2026-04-14` for undated. */
  key: string;
  /** 1-based ordinal within the trip; null for undated chapters (rule 3). */
  ordinal: number | null;
  /** Local calendar date of the chapter (YYYY-MM-DD in viewer TZ). */
  date: string;
  /** True when the group came from takenAt; false when from uploadedAt. */
  dated: boolean;
  /** Earliest item of the day — rendered as the full-width lead plate. */
  lead: T;
  /** The rest of the day's items, ascending. */
  rest: T[];
  /** All items (lead + rest), ascending. */
  items: T[];
  photoCount: number;
  videoCount: number;
}

/** Calendar date (YYYY-MM-DD) of an ISO instant in the viewer's timezone. */
export function localDateOf(iso: string): string {
  const d = new Date(iso);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function effectiveInstant(m: Pick<ChapterMedia, 'takenAt' | 'uploadedAt'>): string {
  return m.takenAt ?? m.uploadedAt;
}

export function groupIntoChapters<T extends ChapterMedia>(media: T[]): Chapter<T>[] {
  const sorted = [...media].sort((a, b) => {
    const ta = Date.parse(effectiveInstant(a));
    const tb = Date.parse(effectiveInstant(b));
    if (ta !== tb) return ta - tb;
    return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
  });

  const dated = new Map<string, T[]>();
  const undated = new Map<string, T[]>();
  for (const m of sorted) {
    if (m.takenAt) {
      const key = localDateOf(m.takenAt);
      (dated.get(key) ?? dated.set(key, []).get(key)!).push(m);
    } else {
      const key = localDateOf(m.uploadedAt);
      (undated.get(key) ?? undated.set(key, []).get(key)!).push(m);
    }
  }

  const datedKeys = [...dated.keys()].sort();
  const undatedKeys = [...undated.keys()].sort();

  // Rule 4: single dated chapter and nothing undated → numeral suppressed.
  const suppressNumeral = datedKeys.length === 1 && undatedKeys.length === 0;

  const chapters: Chapter<T>[] = [];
  datedKeys.forEach((date, i) => {
    const items = dated.get(date)!;
    chapters.push(makeChapter(`d:${date}`, suppressNumeral ? null : i + 1, date, true, items));
  });
  undatedKeys.forEach((date) => {
    const items = undated.get(date)!;
    chapters.push(makeChapter(`u:${date}`, null, date, false, items));
  });
  return chapters;
}

function makeChapter<T extends ChapterMedia>(
  key: string,
  ordinal: number | null,
  date: string,
  dated: boolean,
  items: T[],
): Chapter<T> {
  const [lead, ...rest] = items;
  return {
    key,
    ordinal,
    date,
    dated,
    lead,
    rest,
    items,
    photoCount: items.filter((m) => m.type === 'PHOTO').length,
    videoCount: items.filter((m) => m.type === 'VIDEO').length,
  };
}

/** Find the chapter containing a media id — for "scroll to Day 2" links. */
export function chapterOf<T extends ChapterMedia>(
  chapters: Chapter<T>[],
  mediaId: string,
): Chapter<T> | undefined {
  return chapters.find((c) => c.items.some((m) => m.id === mediaId));
}

/** Two-digit day numeral, or Arabic-Indic when typesetting Arabic display. */
export function formatOrdinal(ordinal: number, arabicIndic = false): string {
  const padded = String(ordinal).padStart(2, '0');
  if (!arabicIndic) return padded;
  const digits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
  return padded.replace(/\d/g, (d) => digits[Number(d)]);
}
