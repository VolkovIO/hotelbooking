import { appConfig } from "../config/appConfig";
import { apiFetch } from "./httpClient";
import type { TimelineEventResponse, UUID } from "./types";

/**
 * Audit service API client.
 *
 * The timeline is read-only. It allows the UI to show what happened to a booking
 * across the distributed flow: booking events, payment events and future events.
 */
export const auditApi = {
  /**
   * GET /api/v1/bookings/{bookingId}/timeline
   */
  getBookingTimeline(bookingId: UUID): Promise<TimelineEventResponse[]> {
    return apiFetch<TimelineEventResponse[]>(
      appConfig.auditServiceBaseUrl,
      `/api/v1/bookings/${bookingId}/timeline`,
    );
  },
};