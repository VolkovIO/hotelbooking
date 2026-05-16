import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

/**
 * Vite development configuration.
 *
 * The browser runs the UI on http://localhost:5173.
 * Backend services run on different ports:
 *
 * - booking-service   -> http://localhost:8080
 * - inventory-service -> http://localhost:8081
 * - audit-service     -> http://localhost:8084
 *
 * Direct browser calls to these ports may be blocked by CORS.
 * For local development we use Vite proxy:
 *
 * UI calls:
 *   /inventory-api/api/v1/hotels
 *
 * Vite forwards them to:
 *   http://localhost:8081/api/v1/hotels
 *
 * This keeps backend services unchanged and makes the demo UI easier to run.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/booking-api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/booking-api/, ""),
      },
      "/inventory-api": {
        target: "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/inventory-api/, ""),
      },
      "/audit-api": {
        target: "http://localhost:8084",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/audit-api/, ""),
      },
    },
  },
});