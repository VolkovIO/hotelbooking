import type { BookingStatus } from "../api";

type StatusBadgeProps = {
  status: BookingStatus;
};

/**
 * Visual badge for booking status.
 *
 * The CSS class name is intentionally booking-status-badge, not just status-badge,
 * because saga status and booking status are different concepts.
 */
export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`booking-status-badge booking-status-badge-${status.toLowerCase()}`}>
      {status}
    </span>
  );
}