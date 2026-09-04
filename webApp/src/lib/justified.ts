/**
 * Justified-row layout — the exact algorithm from Doc 3:
 *
 * Target row height: 150 <480, 180 480–767, 220 768–1279, 260 ≥1280.
 * Gap 3px <768, 4px above. Walk the items, accumulate width/height ratios
 * until the row's natural width at target height exceeds the container,
 * then scale the row to fit exactly. The final row keeps target height and
 * is start-aligned rather than stretched, unless it would exceed
 * 1.5× target. Missing width/height falls back to 4:3.
 *
 * Pure arithmetic on data already in the payload — no measuring, no layout
 * thrash; renders correctly on the first paint from thumbnailUrl alone.
 */

export interface JustifiedItem<T> {
  item: T;
  width: number;
  height: number;
}

export interface JustifiedRow<T> {
  items: { item: T; width: number }[];
  height: number;
  /** Final short row keeps target height and stays start-aligned. */
  justified: boolean;
}

export const FALLBACK_RATIO = 4 / 3;

export function ratioOf(width?: number | null, height?: number | null): number {
  if (!width || !height || width <= 0 || height <= 0) return FALLBACK_RATIO;
  return width / height;
}

export function targetRowHeight(viewportWidth: number): number {
  if (viewportWidth < 480) return 150;
  if (viewportWidth < 768) return 180;
  if (viewportWidth < 1280) return 220;
  return 260;
}

export function rowGap(viewportWidth: number): number {
  return viewportWidth < 768 ? 3 : 4;
}

export function layoutJustifiedRows<T>(
  items: { item: T; ratio: number }[],
  containerWidth: number,
  target: number,
  gap: number,
): JustifiedRow<T>[] {
  if (containerWidth <= 0 || items.length === 0) return [];

  const rows: JustifiedRow<T>[] = [];
  let row: { item: T; ratio: number }[] = [];
  let ratioSum = 0;

  const flush = (isLast: boolean) => {
    if (row.length === 0) return;
    const gaps = gap * (row.length - 1);
    const natural = ratioSum * target + gaps;

    if (!isLast || natural >= containerWidth) {
      // Scale to fit the container exactly.
      const height = (containerWidth - gaps) / ratioSum;
      rows.push({
        items: row.map((r) => ({ item: r.item, width: r.ratio * height })),
        height,
        justified: true,
      });
    } else {
      // Final short row: keep target height, start-aligned — unless the
      // scaled height would stay under 1.5× target, in which case justify.
      const scaledHeight = (containerWidth - gaps) / ratioSum;
      if (scaledHeight <= target * 1.5) {
        rows.push({
          items: row.map((r) => ({ item: r.item, width: r.ratio * scaledHeight })),
          height: scaledHeight,
          justified: true,
        });
      } else {
        rows.push({
          items: row.map((r) => ({ item: r.item, width: r.ratio * target })),
          height: target,
          justified: false,
        });
      }
    }
    row = [];
    ratioSum = 0;
  };

  for (const entry of items) {
    row.push(entry);
    ratioSum += entry.ratio;
    const natural = ratioSum * target + gap * (row.length - 1);
    if (natural >= containerWidth) flush(false);
  }
  flush(true);

  return rows;
}
