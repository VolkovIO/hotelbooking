import { appConfig } from "./config/appConfig";
import { HotelsPage } from "./pages/HotelsPage";

/**
 * App is the root React component.
 *
 * For now we keep navigation intentionally simple and render only one page.
 * Later we will add small navigation between:
 *
 * - Hotels
 * - My bookings
 * - Inventory admin
 * - Observability
 */
function App() {
  return (
    <main className="app-shell">
      <header className="top-bar">
        <div>
          <div className="hero-badge">Hotel Booking Demo UI</div>
          <h1 className="app-title">Distributed hotel booking platform</h1>
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

      <HotelsPage />
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