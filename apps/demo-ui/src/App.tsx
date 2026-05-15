import { useState } from "react";
import { appConfig } from "./config/appConfig";
import { HotelsPage } from "./pages/HotelsPage";
import { MyBookingsPage } from "./pages/MyBookingsPage";

type AppPage = "hotels" | "my-bookings";

/**
 * App is the root React component.
 *
 * We intentionally do not add React Router yet.
 * For this small demo UI a local page state is enough:
 *
 * - hotels
 * - my-bookings
 *
 * Later, if the UI grows, this can be replaced with proper routing.
 */
function App() {
  const [currentPage, setCurrentPage] = useState<AppPage>("hotels");

  return (
    <main className="app-shell">
      <header className="top-bar">
        <div>
          <div className="hero-badge">Hotel Booking Demo UI</div>
          <h1 className="app-title">Distributed hotel booking platform</h1>
          <p className="app-subtitle">
            Demo client for booking saga, inventory availability, payment flow and audit timeline.
          </p>
        </div>

        <div className="auth-card">
          <span>Auth mode</span>
          <strong>{appConfig.authMode}</strong>
          <small>{appConfig.demoUserEmail}</small>
        </div>
      </header>

      <section className="service-strip">
        <ServiceLink label="Booking" value={appConfig.bookingServiceBaseUrl} />
        <ServiceLink label="Inventory" value={appConfig.inventoryServiceBaseUrl} />
        <ServiceLink label="Audit" value={appConfig.auditServiceBaseUrl} />
      </section>

      <nav className="main-navigation" aria-label="Demo UI navigation">
        <button
          className={currentPage === "hotels" ? "nav-tab nav-tab-active" : "nav-tab"}
          type="button"
          onClick={() => setCurrentPage("hotels")}
        >
          <span className="nav-tab-icon">🏨</span>
          <span>
            <strong>Hotels</strong>
            <small>Catalog & booking saga</small>
          </span>
        </button>

        <button
          className={currentPage === "my-bookings" ? "nav-tab nav-tab-active" : "nav-tab"}
          type="button"
          onClick={() => setCurrentPage("my-bookings")}
        >
          <span className="nav-tab-icon">📋</span>
          <span>
            <strong>My bookings</strong>
            <small>Status, timeline & cancel</small>
          </span>
        </button>
      </nav>

      {currentPage === "hotels" && <HotelsPage />}
      {currentPage === "my-bookings" && <MyBookingsPage />}
    </main>
  );
}

type ServiceLinkProps = {
  label: string;
  value: string;
};

function ServiceLink({ label, value }: ServiceLinkProps) {
  return (
    <div className="service-link">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export default App;