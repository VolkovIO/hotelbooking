import { useState } from "react";
import { AuthPanel } from "./components/AuthPanel";
import { HotelsPage } from "./pages/HotelsPage";
import { InventoryAdminPage } from "./pages/InventoryAdminPage";
import { MyBookingsPage } from "./pages/MyBookingsPage";

type AppPage = "hotels" | "my-bookings" | "inventory-admin";

function App() {
  const [currentPage, setCurrentPage] = useState<AppPage>("hotels");

  return (
    <main className="app-shell">
      <header className="top-bar">
        <div>
          <div className="hero-badge">Hotel Booking Demo UI</div>
        </div>

        <AuthPanel />
      </header>

      <nav className="main-navigation main-navigation-three" aria-label="Demo UI navigation">
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

export default App;