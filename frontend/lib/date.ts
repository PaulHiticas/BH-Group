/**
 * Formats a calendar date (year/month/day only - no time, no timezone) as
 * "YYYY-MM-DD" using the date's *local* components.
 *
 * Do not use `date.toISOString().slice(0, 10)` for this: `toISOString()`
 * converts to UTC first, and Romania is UTC+2/+3, so local midnight rolls
 * back to the previous day once converted (e.g. picking Aug 14 would
 * silently produce "2026-08-13"). Anything that represents a plain
 * calendar date - a check-in/check-out day, an `<input type="date">`
 * value, an expense/accounting date - should go through this instead.
 *
 * Not for `Instant`/timestamp values, which are correctly UTC already.
 */
export function formatLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

/** Inverse of formatLocalDate: parses "YYYY-MM-DD" as a local calendar date (midnight local time, not UTC). */
export function parseLocalDate(iso: string): Date {
  const [year, month, day] = iso.split("-").map(Number)
  return new Date(year, month - 1, day)
}
