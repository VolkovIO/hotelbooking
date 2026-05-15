import type { BookingStatus } from "../api";

type StatusBadgeProps = {
  status: BookingStatus;
};

/**
 * Small visual component for booking status.
 *
 * We keep status-to-style mapping in one place instead of scattering
 * conditional CSS class names across pages.
 */
export function StatusBadge({ status }: StatusBadgeProps) {
  return <span className={`status-badge status-badge-${status.toLowerCase()}`}>{status}</span>;
}