import { useState } from "react";
import { appConfig } from "./config/appConfig";
import { HotelsPage } from "./pages/HotelsPage";
import { InventoryAdminPage } from "./pages/InventoryAdminPage";
import { MyBookingsPage } from "./pages/MyBookingsPage";

type AppPage = "hotels" | "my-bookings" | "inventory-admin";

function App() {
  const [currentPage, setCurrentPage] = useState<AppPage>("hotels");

  return (
    <main className="app-shell">
      <header className="top-bar top-bar-compact">
        <div>
          <div className="hero-badge">Hotel Booking Demo UI</div>
          <div className="app-highlights" aria-label="Demo capabilities">
            <span>Booking saga</span>
            <span>Inventory availability</span>
            <span>Payment flow</span>
            <span>Audit timeline</span>
          </div>
        </div>

        <div className="auth-card auth-card-compact">
          <span>Auth</span>
          <strong>{appConfig.authMode}</strong>
          <small>{appConfig.demoUserEmail}</small>
        </div>
      </header>

      <details className="runtime-details">
        <summary>Runtime service routes</summary>

        <section className="service-strip service-strip-compact">
          <ServiceLink label="Booking" value={appConfig.bookingServiceBaseUrl} />
          <ServiceLink label="Inventory" value={appConfig.inventoryServiceBaseUrl} />
          <ServiceLink label="Audit" value={appConfig.auditServiceBaseUrl} />
        </section>
      </details>

      <nav className="main-navigation main-navigation-three main-navigation-compact" aria-label="Demo UI navigation">
        <button
          className={currentPage === "hotels" ? "nav-tab nav-tab-active" : "nav-tab"}
          type="button"
          onClick={() => setCurrentPage("hotels")}
        >
          <span className="nav-tab-icon">🏨</span>
          <span>
            <strong>Hotels</strong>
            <small>Catalog & booking</small>
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
            <small>Status & timeline</small>
          </span>
        </button>

        <button
          className={currentPage === "inventory-admin" ? "nav-tab nav-tab-active" : "nav-tab"}
          type="button"
          onClick={() => setCurrentPage("inventory-admin")}
        >
          <span className="nav-tab-icon">🛠️</span>
          <span>
            <strong>Inventory Admin</strong>
            <small>Hotels & availability</small>
          </span>
        </button>
      </nav>

      {currentPage === "hotels" && <HotelsPage />}
      {currentPage === "my-bookings" && <MyBookingsPage />}
      {currentPage === "inventory-admin" && <InventoryAdminPage />}
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