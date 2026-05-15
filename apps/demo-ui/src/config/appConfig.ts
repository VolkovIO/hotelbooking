/**
 * Central place for demo UI runtime configuration.
 *
 * In Java terms, this file plays a role similar to application.yaml:
 * it keeps external service URLs and demo settings outside of React components.
 *
 * For the first UI step we do not call backend services yet,
 * but defining these URLs now makes the next steps easier:
 *
 * - booking-service: creates bookings and returns current user's bookings
 * - inventory-service: hotels, room types and availability
 * - audit-service: booking timeline
 */
export const appConfig = {
  bookingServiceBaseUrl:
    import.meta.env.VITE_BOOKING_SERVICE_BASE_URL ?? "http://localhost:8080",

  inventoryServiceBaseUrl:
    import.meta.env.VITE_INVENTORY_SERVICE_BASE_URL ?? "http://localhost:8081",

  auditServiceBaseUrl:
    import.meta.env.VITE_AUDIT_SERVICE_BASE_URL ?? "http://localhost:8084",

  /**
   * For the first version we use the backend dev profile.
   *
   * In dev mode booking-service resolves the current user internally
   * as the fixed demo user. The frontend does not send Authorization header yet.
   */
  authMode: import.meta.env.VITE_AUTH_MODE ?? "demo",

  demoUserEmail: "dev@example.com",
} as const;