export type AuthMode = "demo" | "google";

function resolveAuthMode(value: string | undefined): AuthMode {
  return value === "google" ? "google" : "demo";
}

export const appConfig = {
  bookingServiceBaseUrl:
    import.meta.env.VITE_BOOKING_SERVICE_BASE_URL ?? "/booking-api",

  inventoryServiceBaseUrl:
    import.meta.env.VITE_INVENTORY_SERVICE_BASE_URL ?? "/inventory-api",

  auditServiceBaseUrl:
    import.meta.env.VITE_AUDIT_SERVICE_BASE_URL ?? "/audit-api",

  authMode: resolveAuthMode(import.meta.env.VITE_AUTH_MODE),

  googleClientId: import.meta.env.VITE_GOOGLE_CLIENT_ID ?? "",

  demoUserEmail: "dev@example.com",
} as const;