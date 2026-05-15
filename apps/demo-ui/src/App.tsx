import { appConfig } from "./config/appConfig";

/**
 * App is the root React component.
 *
 * A React component is simply a TypeScript function that returns JSX.
 * JSX looks like HTML, but it is actually TypeScript/JavaScript syntax
 * that React turns into UI elements.
 *
 * This first version intentionally does not call backend services yet.
 * The goal of this commit is only to add a clean UI shell for the future demo.
 */
function App() {
  return (
    <main className="app-shell">
      <section className="hero-card">
        <div className="hero-badge">Hotel Booking Demo UI</div>

        <h1>Distributed hotel booking platform</h1>

        <p className="hero-text">
          Thin React demo client for the backend system: booking saga,
          inventory availability, payments, audit timeline and notifications.
        </p>

        <div className="status-grid">
          <InfoCard
            title="Auth mode"
            value={appConfig.authMode}
            description={`Current local demo user: ${appConfig.demoUserEmail}`}
          />

          <InfoCard
            title="Booking service"
            value={appConfig.bookingServiceBaseUrl}
            description="Booking saga, current user bookings and cancellation."
          />

          <InfoCard
            title="Inventory service"
            value={appConfig.inventoryServiceBaseUrl}
            description="Hotels, room types, availability and admin operations."
          />

          <InfoCard
            title="Audit service"
            value={appConfig.auditServiceBaseUrl}
            description="Timeline of booking and payment events."
          />
        </div>

        <div className="next-steps">
          <h2>Next UI steps</h2>

          <ol>
            <li>Add backend API clients with typed DTOs.</li>
            <li>Show hotel catalog from inventory-service.</li>
            <li>Create booking through the saga endpoint.</li>
            <li>Show current user bookings and audit timeline.</li>
          </ol>
        </div>
      </section>
    </main>
  );
}

/**
 * Props are input parameters for a React component.
 *
 * This is similar to a small immutable DTO in Java:
 * the parent component passes title/value/description,
 * and InfoCard only renders them.
 */
type InfoCardProps = {
  title: string;
  value: string;
  description: string;
};

function InfoCard({ title, value, description }: InfoCardProps) {
  return (
    <article className="info-card">
      <h2>{title}</h2>
      <p className="info-value">{value}</p>
      <p className="info-description">{description}</p>
    </article>
  );
}

export default App;