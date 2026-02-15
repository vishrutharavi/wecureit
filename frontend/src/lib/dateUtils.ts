/**
 * Date utility functions for consistent local date handling across the application.
 *
 * IMPORTANT: These functions ensure dates are handled in the user's local timezone
 * and prevent timezone-related date shifts that can occur when using Date.toISOString()
 */

/**
 * Convert a Date object to YYYY-MM-DD format using local timezone.
 *
 * @param date - The Date object to convert
 * @returns ISO date string in YYYY-MM-DD format (local timezone)
 *
 * @example
 * const date = new Date(2026, 1, 23); // Feb 23, 2026 local time
 * toLocalIso(date); // "2026-02-23"
 */
export function toLocalIso(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Parse a YYYY-MM-DD date string as a local Date object.
 *
 * This avoids UTC parsing that occurs with new Date("YYYY-MM-DD"), which can
 * shift the date by +/- 1 day depending on the user's timezone.
 *
 * @param iso - ISO date string in YYYY-MM-DD format
 * @returns Date object in local timezone at midnight
 *
 * @example
 * parseLocalDate("2026-02-23"); // Feb 23, 2026 00:00:00 local time
 */
export function parseLocalDate(iso: string | null): Date {
  if (!iso) return new Date();

  const match = iso.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return new Date(iso);

  const y = parseInt(match[1], 10);
  const m = parseInt(match[2], 10) - 1; // Month is 0-indexed
  const d = parseInt(match[3], 10);

  return new Date(y, m, d);
}

/**
 * Format a Date object as YYYY-MM-DDTHH:MM:SS in local timezone.
 *
 * @param date - The Date object to format
 * @returns ISO datetime string (local timezone, no 'Z' suffix)
 *
 * @example
 * const date = new Date(2026, 1, 23, 14, 30); // Feb 23, 2026 2:30 PM local
 * formatLocalDateTime(date); // "2026-02-23T14:30:00"
 */
export function formatLocalDateTime(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  const s = String(date.getSeconds()).padStart(2, '0');

  return `${y}-${m}-${d}T${h}:${min}:${s}`;
}

/**
 * Get today's date in YYYY-MM-DD format (local timezone).
 *
 * @returns Today's date as ISO string
 *
 * @example
 * getTodayLocal(); // "2026-02-14"
 */
export function getTodayLocal(): string {
  return toLocalIso(new Date());
}
