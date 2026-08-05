import type { RiskLevel } from "@/lib/types";
import { RISK_ORDER } from "@/lib/risk";

const DAY_MS = 86_400_000;

function startOfDayMs(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

function daysBetween(laterMs: number, earlierMs: number): number {
  return Math.round((laterMs - earlierMs) / DAY_MS);
}

export function trendWindow(days: number, now: Date = new Date()): { start: Date; end: Date } {
  const start = new Date(now);
  start.setDate(start.getDate() - (days - 1));
  return { start, end: now };
}

export function bucketDailyCounts(timestamps: string[], days: number, now: Date = new Date()): number[] {
  const counts = new Array(days).fill(0) as number[];
  const todayMs = startOfDayMs(now);

  for (const timestamp of timestamps) {
    const dayMs = startOfDayMs(new Date(timestamp));
    const index = days - 1 - daysBetween(todayMs, dayMs);
    if (index >= 0 && index < days) counts[index] += 1;
  }

  return counts;
}

export function bucketCumulativeCounts(timestamps: string[], days: number, now: Date = new Date()): number[] {
  const daily = bucketDailyCounts(timestamps, days, now);
  const windowStartMs = startOfDayMs(now) - (days - 1) * DAY_MS;
  const before = timestamps.filter((timestamp) => startOfDayMs(new Date(timestamp)) < windowStartMs).length;

  const cumulative = new Array(days).fill(0) as number[];
  let running = before;
  for (let i = 0; i < days; i++) {
    running += daily[i];
    cumulative[i] = running;
  }
  return cumulative;
}

export function averageScore(entries: { lastScore: number | null }[]): number | null {
  const scores = entries.map((entry) => entry.lastScore).filter((score): score is number => score !== null);
  if (scores.length === 0) return null;
  return Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length);
}

export function dominantRiskLevel(entries: { lastRiskLevel: RiskLevel | null }[]): RiskLevel | null {
  let best: RiskLevel | null = null;
  let bestCount = 0;

  for (const level of RISK_ORDER) {
    const count = entries.filter((entry) => entry.lastRiskLevel === level).length;
    if (count > 0 && count >= bestCount) {
      best = level;
      bestCount = count;
    }
  }

  return best;
}
