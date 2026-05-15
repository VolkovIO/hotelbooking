import { useEffect, useState } from "react";
import { inventoryApi, type HotelSummaryResponse } from "../api";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingState } from "../components/LoadingState";

/**
 * HotelsPage is the first real backend-integrated screen.
 *
 * It loads hotel summaries from inventory-service:
 *
 *   GET /api/v1/hotels?limit=20
 *
 * React state used here:
 *
 * - hotels: successfully loaded data
 * - loading: whether request is currently in progress
 * - error: failed request details
 *
 * For a Java developer this is similar to a small controller/view-model pair:
 * the component starts a request, stores result in state, and renders different
 * UI depending on the state.
 */
export function HotelsPage() {
  const [hotels, setHotels] = useState<HotelSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  /**
   * useEffect runs after the component is rendered.
   *
   * With an empty dependency array [] it runs only once when the page is mounted.
   * This is a common place to load initial data from backend.
   */
  useEffect(() => {
    let cancelled = false;

    async function loadHotels() {
      try {
        setLoading(true);
        setError(null);

        const result = await inventoryApi.findHotels(20);

        /**
         * The cancelled flag protects us from updating state after the component
         * was unmounted. This is a small safety pattern for async React effects.
         */
        if (!cancelled) {
          setHotels(result);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadHotels();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="page-section">
      <div className="page-header">
        <div>
          <p className="eyebrow">Inventory catalog</p>
          <h1>Hotels</h1>
          <p className="page-description">
            Public hotel catalog loaded from inventory-service. This is the
            first visible step of the booking demo flow.
          </p>
        </div>

        <button className="secondary-button" type="button" onClick={() => window.location.reload()}>
          Reload
        </button>
      </div>

      {loading && <LoadingState text="Loading hotels from inventory-service..." />}

      {!loading && error !== null && <ErrorMessage error={error} />}

      {!loading && error === null && hotels.length === 0 && (
        <div className="state-card">
          No hotels found. Check Mongo demo data or use the future Inventory Admin page.
        </div>
      )}

      {!loading && error === null && hotels.length > 0 && (
        <div className="hotel-grid">
          {hotels.map((hotel) => (
            <article className="hotel-card" key={hotel.hotelId}>
              <div>
                <p className="eyebrow">{hotel.city}</p>
                <h2>{hotel.name}</h2>
              </div>

              <div className="room-type-list">
                {hotel.roomTypes.length === 0 ? (
                  <p className="muted-text">No room types registered</p>
                ) : (
                  hotel.roomTypes.map((roomType) => (
                    <div className="room-type-pill" key={roomType.roomTypeId}>
                      <span>{roomType.name}</span>
                      <strong>{roomType.guestCapacity} guests</strong>
                    </div>
                  ))
                )}
              </div>

              <div className="technical-id">
                <span>hotelId</span>
                <code>{hotel.hotelId}</code>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}