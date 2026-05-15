/**
 * Shortens long technical identifiers for UI tables.
 *
 * Full UUIDs are important for debugging, but rendering them fully inside
 * every table cell makes the interface noisy and too wide.
 *
 * Example:
 *   3e4edbe6-e9c8-42a2-bb8a-cba0a8920242
 * becomes:
 *   3e4edbe6…0242
 */
export function shortTechnicalId(value: string): string {
  if (value.length <= 16) {
    return value;
  }

  return `${value.slice(0, 8)}…${value.slice(-4)}`;
}