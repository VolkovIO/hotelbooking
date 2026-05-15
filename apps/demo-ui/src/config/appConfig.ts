/**
 * Authentication mode supported by the demo UI.
 *
 * demo:
 *   booking-service runs with the dev security profile and resolves the current user
 *   internally as dev@example.com. The frontend does not send Authorization header.
 *
 * google:
 *   the frontend receives a Google ID token and sends it to booking-service as
 *   Authorization: Bearer <token>. We will add this later.
 */
export type AuthMode = "demo" | "google";

function resolveAuthMode(value: string | undefined): AuthMode {
  return value === "google" ? "google" : "demo";
}

/**
 * Central place for demo UI runtime configuration.
 *
 * In Java terms, this file plays a role similar to application.yaml:
 * it keeps external service URLs and demo settings outside of React components.
 */
export const appConfig = {
  bookingServiceBaseUrl:
    import.meta.env.VITE_BOOKING_SERVICE_BASE_URL ?? "http://localhost:8080",

  inventoryServiceBaseUrl:
    import.meta.env.VITE_INVENTORY_SERVICE_BASE_URL ?? "http://localhost:8081",

  auditServiceBaseUrl:
    import.meta.env.VITE_AUDIT_SERVICE_BASE_URL ?? "http://localhost:8084",

  authMode: resolveAuthMode(import.meta.env.VITE_AUTH_MODE),

  demoUserEmail: "dev@example.com",
} as const;