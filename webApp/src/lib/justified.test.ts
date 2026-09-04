import { describe, expect, it } from 'vitest';
import {
  layoutJustifiedRows,
  ratioOf,
  rowGap,
  targetRowHeight,
  FALLBACK_RATIO,
} from './justified';

const items = (ratios: number[]) => ratios.map((ratio, i) => ({ item: i, ratio }));

describe('targetRowHeight — 150 / 180 / 220 / 260 by viewport', () => {
  it.each([
    [479, 150],
    [480, 180],
    [767, 180],
    [768, 220],
    [1279, 220],
    [1280, 260],
  ])('viewport %i → %i', (vw, expected) => {
    expect(targetRowHeight(vw)).toBe(expected);
  });
});

describe('rowGap — 3px below 768, 4px above', () => {
  it('returns 3 below 768 and 4 at 768+', () => {
    expect(rowGap(767)).toBe(3);
    expect(rowGap(768)).toBe(4);
  });
});

describe('ratioOf — missing dimensions fall back to 4:3', () => {
  it('uses real dimensions when present', () => {
    expect(ratioOf(1600, 900)).toBeCloseTo(16 / 9);
  });
  it.each([
    [undefined, undefined],
    [null, 900],
    [1600, null],
    [0, 900],
    [1600, -1],
  ])('falls back for %o × %o', (w, h) => {
    expect(ratioOf(w as never, h as never)).toBe(FALLBACK_RATIO);
  });
});

describe('layoutJustifiedRows', () => {
  const WIDTH = 1000;
  const TARGET = 220;
  const GAP = 4;

  it('scales full rows to fit the container exactly', () => {
    const rows = layoutJustifiedRows(items([1.5, 1, 0.75, 1.3, 1, 1.4]), WIDTH, TARGET, GAP);
    for (const row of rows.slice(0, -1)) {
      const total =
        row.items.reduce((sum, i) => sum + i.width, 0) + GAP * (row.items.length - 1);
      expect(total).toBeCloseTo(WIDTH, 3);
      expect(row.justified).toBe(true);
    }
  });

  it('keeps the final short row at target height, start-aligned', () => {
    // One landscape item alone: natural width ≈ 1.5×220 = 330 ≪ 1000; scaling
    // it to fit would blow past 1.5× target, so it stays at target height.
    const rows = layoutJustifiedRows(items([1.5]), WIDTH, TARGET, GAP);
    expect(rows).toHaveLength(1);
    expect(rows[0].height).toBe(TARGET);
    expect(rows[0].justified).toBe(false);
    expect(rows[0].items[0].width).toBeCloseTo(1.5 * TARGET);
  });

  it('justifies a final row that is close enough (≤1.5× target when scaled)', () => {
    // Three 1.5-ratio items: natural 3*1.5*220 + 8 = 998 < 1000 → last row,
    // scaled height (1000-8)/4.5 ≈ 220.4 ≤ 330 → justified.
    const rows = layoutJustifiedRows(items([1.5, 1.5, 1.5]), WIDTH, TARGET, GAP);
    expect(rows).toHaveLength(1);
    expect(rows[0].justified).toBe(true);
    expect(rows[0].height).toBeLessThanOrEqual(TARGET * 1.5);
  });

  it('handles empty input and zero width', () => {
    expect(layoutJustifiedRows([], WIDTH, TARGET, GAP)).toEqual([]);
    expect(layoutJustifiedRows(items([1]), 0, TARGET, GAP)).toEqual([]);
  });

  it('never drops an item', () => {
    const ratios = Array.from({ length: 37 }, (_, i) => 0.6 + (i % 5) * 0.35);
    const rows = layoutJustifiedRows(items(ratios), WIDTH, TARGET, GAP);
    const count = rows.reduce((sum, r) => sum + r.items.length, 0);
    expect(count).toBe(37);
  });

  it('a portrait-heavy row grows narrower items proportionally', () => {
    const rows = layoutJustifiedRows(items([0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75]), WIDTH, TARGET, GAP);
    // First row must be justified with equal widths.
    const first = rows[0];
    const widths = first.items.map((i) => i.width);
    for (const w of widths) expect(w).toBeCloseTo(widths[0], 5);
  });
});
