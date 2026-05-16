import { appConfig } from "../config/appConfig";
import { apiFetch } from "./httpClient";
import type {
  BookingPageResponse,
  BookingResponse,
  BookingSagaResponse,
  StartBookingSagaRequest,
  UUID,
} from "./types";

export type SagaEngine = "handmade" | "spring-statemachine";

export type GetMyBookingsParams = {
  page?: number;
  size?: number;
  authToken?: string | null;
};

/**
 * Booking service API client.
 *
 * This module mirrors booking-service REST endpoints and hides raw HTTP details
 * from React components.
 */
export const bookingApi = {
  /**
   * GET /api/v1/bookings/my?page=0&size=20
   *
   * Returns bookings of the current user.
   * In demo auth mode the backend resolves current user as dev@example.com.
   */
  getMyBookings({
    page = 0,
    size = 20,
    authToken,
  }: GetMyBookingsParams = {}): Promise<BookingPageResponse> {
    return apiFetch<BookingPageResponse>(
      appConfig.bookingServiceBaseUrl,
      "/api/v1/bookings/my",
      {
        query: { page, size },
        authToken,
      },
    );
  },

  /**
   * GET /api/v1/bookings/{bookingId}
   */
  getBookingById(
    bookingId: UUID,
    authToken?: string | null,
  ): Promise<BookingResponse> {
    return apiFetch<BookingResponse>(
      appConfig.bookingServiceBaseUrl,
      `/api/v1/bookings/${bookingId}`,
      { authToken },
    );
  },

  /**
   * POST /api/v1/bookings/saga
   * or
   * POST /api/v1/bookings/saga-statemachine
   *
   * The handmade saga is the main production-like implementation.
   * Spring Statemachine is kept as a prototype/comparison option.
   */
  startBookingSaga(
    request: StartBookingSagaRequest,
    engine: SagaEngine = "handmade",
    authToken?: string | null,
  ): Promise<BookingSagaResponse> {
    const path =
      engine === "spring-statemachine"
        ? "/api/v1/bookings/saga-statemachine"
        : "/api/v1/bookings/saga";

    return apiFetch<BookingSagaResponse>(appConfig.bookingServiceBaseUrl, path, {
      method: "POST",
      body: request,
      authToken,
    });
  },

  /**
   * POST /api/v1/bookings/{bookingId}/cancel
   */
  cancelBooking(
    bookingId: UUID,
    authToken?: string | null,
  ): Promise<BookingResponse> {
    return apiFetch<BookingResponse>(
      appConfig.bookingServiceBaseUrl,
      `/api/v1/bookings/${bookingId}/cancel`,
      {
        method: "POST",
        authToken,
      },
    );
  },
};