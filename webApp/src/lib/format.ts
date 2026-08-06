import type { Lang } from '../i18n/LocaleProvider';

const LOCALE: Record<Lang, string> = { en: 'en-GB', ar: 'ar' };

/** Locale for dates — Western digits in both languages (Doc 2 numeral rule). */
function dateLocale(lang: Lang): string {
  return lang === 'ar' ? 'ar-u-nu-latn' : LOCALE.en;
}

/** "Sat 12 Apr" — chapter headers. */
export function formatDayShort(isoOrDate: string, lang: Lang = 'en'): string {
  const d = new Date(isoOrDate.length === 10 ? `${isoOrDate}T12:00:00` : isoOrDate);
  return new Intl.DateTimeFormat(dateLocale(lang), {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(d);
}

/** "12 April" / "19 Apr 2026" style medium date. */
export function formatDateMedium(iso: string, lang: Lang = 'en'): string {
  return new Intl.DateTimeFormat(dateLocale(lang), {
    day: 'numeric',
    month: 'long',
  }).format(new Date(iso));
}

export function formatDateFull(iso: string, lang: Lang = 'en'): string {
  return new Intl.DateTimeFormat(dateLocale(lang), {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(iso));
}

/** "Apr 2026" — card meta. */
export function formatMonthYear(iso: string, lang: Lang = 'en'): string {
  return new Intl.DateTimeFormat(dateLocale(lang), {
    month: 'short',
    year: 'numeric',
  }).format(new Date(iso));
}

/** "Sat 12 Apr, 17:42" — lightbox info. */
export function formatDateTime(iso: string, lang: Lang = 'en'): string {
  return new Intl.DateTimeFormat(dateLocale(lang), {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(iso));
}

/** "4.2 MB" — humanised file size. */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
}

/** "0:14" / "2:05" — video duration badge from durationMs. */
export function formatDuration(durationMs: number): string {
  const totalSeconds = Math.round(durationMs / 1000);
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export type RelativeTimeParts =
  | { kind: 'justNow' }
  | { kind: 'minutes'; n: number }
  | { kind: 'hours'; n: number }
  | { kind: 'yesterday' }
  | { kind: 'days'; n: number }
  | { kind: 'date'; iso: string };

/** Local midnight — so "yesterday" means the calendar day before, not 24 hours. */
function startOfLocalDay(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

/** Structured relative time — the caller renders it through i18n. */
export function relativeTimeParts(iso: string, now: Date = new Date()): RelativeTimeParts {
  const then = new Date(iso);
  const diffMs = now.getTime() - then.getTime();
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return { kind: 'justNow' };
  if (minutes < 60) return { kind: 'minutes', n: minutes };
  const hours = Math.floor(minutes / 60);
  if (hours < 24 && now.getDate() === then.getDate()) return { kind: 'hours', n: hours };
  // Calendar days apart, rounded to survive DST — 47 hours ago is two days ago,
  // not yesterday.
  const days = Math.round((startOfLocalDay(now) - startOfLocalDay(then)) / 86_400_000);
  if (days <= 1) return { kind: 'yesterday' };
  if (days < 7) return { kind: 'days', n: days };
  return { kind: 'date', iso };
}

/** Time-of-day bucket for the /trips greeting. */
export function timeOfDay(now: Date = new Date()): 'morning' | 'afternoon' | 'evening' {
  const h = now.getHours();
  if (h < 12) return 'morning';
  if (h < 17) return 'afternoon';
  return 'evening';
}

/** Weekday name for the greeting micro-label ("Tuesday morning"). */
export function weekdayName(lang: Lang = 'en', now: Date = new Date()): string {
  return new Intl.DateTimeFormat(dateLocale(lang), { weekday: 'long' }).format(now);
}
