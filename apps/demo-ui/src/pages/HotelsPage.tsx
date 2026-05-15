import { useEffect, useMemo, useState } from "react";
import {
  inventoryApi,
  type HotelResponse,
  type HotelSummaryResponse,
  type RoomAvailabilityResponse,
  type RoomTypeResponse,
  type UUID,
} from "../api";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingState } from "../components/LoadingState";

/**
 * Default demo dates.
 *
 * They match our local demo data style where availability is usually created
 * for future dates such as 2030-06-11.
 */
const DEFAULT_FROM = "2030-06-11";
const DEFAULT_TO = "2030-06-12";

/**
 * HotelsPage is the first real backend-integrated screen.
 *
 * It now supports two related flows:
 *
 * 1. Hotel catalog:
 *    GET /api/v1/hotels?limit=20
 *
 * 2. Hotel details and room availability:
 *    GET /api/v1/hotels/{hotelId}
 *    GET /api/v1/hotels/{hotelId}/room-types/{roomTypeId}/availability?from=...&to=...
 *
 * We still keep this in one page to avoid introducing routing too early.
 * Later we can split it into HotelsPage, HotelDetailsPage and BookingForm.
 */
export function HotelsPage() {
  const [hotels, setHotels] = useState<HotelSummaryResponse[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState<unknown>(null);

  const [selectedHotelId, setSelectedHotelId] = useState<UUID | null>(null);
  const [selectedHotel, setSelectedHotel] = useState<HotelResponse | null>(null);
  const [hotelDetailsLoading, setHotelDetailsLoading] = useState(false);
  const [hotelDetailsError, setHotelDetailsError] = useState<unknown>(null);

  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<UUID | null>(null);
  const [availabilityFrom, setAvailabilityFrom] = useState(DEFAULT_FROM);
  const [availabilityTo, setAvailabilityTo] = useState(DEFAULT_TO);
  const [availability, setAvailability] = useState<RoomAvailabilityResponse[]>([]);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [availabilityError, setAvailabilityError] = useState<unknown>(null);

  const selectedRoomType = useMemo(
    () =>
      selectedHotel?.roomTypes.find((roomType) => roomType.roomTypeId === selectedRoomTypeId) ??
      null,
    [selectedHotel, selectedRoomTypeId],
  );

  /**
   * Initial catalog loading.
   *
   * useEffect with [] runs once when the component is mounted.
   */
  useEffect(() => {
    let cancelled = false;

    async function loadHotels() {
      try {
        setCatalogLoading(true);
        setCatalogError(null);

        const result = await inventoryApi.findHotels(20);

        if (!cancelled) {
          setHotels(result);
        }
      } catch (requestError) {
        if (!cancelled) {
          setCatalogError(requestError);
        }
      } finally {
        if (!cancelled) {
          setCatalogLoading(false);
        }
      }
    }

    loadHotels();

    return () => {
      cancelled = true;
    };
  }, []);

  async function openHotelDetails(hotelId: UUID) {
    try {
      setSelectedHotelId(hotelId);
      setSelectedHotel(null);
      setSelectedRoomTypeId(null);
      setAvailability([]);
      setHotelDetailsLoading(true);
      setHotelDetailsError(null);
      setAvailabilityError(null);

      const hotel = await inventoryApi.getHotelById(hotelId);

      setSelectedHotel(hotel);

      /**
       * Small usability shortcut:
       * if the hotel has at least one room type, select the first one automatically.
       */
      setSelectedRoomTypeId(hotel.roomTypes[0]?.roomTypeId ?? null);
    } catch (requestError) {
      setHotelDetailsError(requestError);
    } finally {
      setHotelDetailsLoading(false);
    }
  }

  async function checkAvailability() {
    if (selectedHotel === null || selectedRoomTypeId === null) {
      return;
    }

    try {
      setAvailabilityLoading(true);
      setAvailabilityError(null);
      setAvailability([]);

      const result = await inventoryApi.getRoomAvailability(
        selectedHotel.hotelId,
        selectedRoomTypeId,
        availabilityFrom,
        availabilityTo,
      );

      setAvailability(result);
    } catch (requestError) {
      setAvailabilityError(requestError);
    } finally {
      setAvailabilityLoading(false);
    }
  }

  return (
    <section className="page-section">
      <div className="page-header">
        <div>
          <p className="eyebrow">Inventory catalog</p>
          <h1>Hotels</h1>
          <p className="page-description">
            Select a hotel, inspect its room types and check availability before
            starting the booking saga.
          </p>
        </div>

        <button className="secondary-button" type="button" onClick={() => window.location.reload()}>
          Reload
        </button>
      </div>

      {catalogLoading && <LoadingState text="Loading hotels from inventory-service..." />}

      {!catalogLoading && catalogError !== null && <ErrorMessage error={catalogError} />}

      {!catalogLoading && catalogError === null && hotels.length === 0 && (
        <div className="state-card">
          No hotels found. Check Mongo demo data or use the future Inventory Admin page.
        </div>
      )}

      {!catalogLoading && catalogError === null && hotels.length > 0 && (
        <div className="hotel-grid">
          {hotels.map((hotel) => (
            <article
              className={
                hotel.hotelId === selectedHotelId ? "hotel-card hotel-card-selected" : "hotel-card"
              }
              key={hotel.hotelId}
            >
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

              <button
                className="primary-button"
                type="button"
                onClick={() => openHotelDetails(hotel.hotelId)}
              >
                Open details
              </button>
            </article>
          ))}
        </div>
      )}

      <HotelDetailsPanel
        selectedHotel={selectedHotel}
        loading={hotelDetailsLoading}
        error={hotelDetailsError}
        selectedRoomTypeId={selectedRoomTypeId}
        selectedRoomType={selectedRoomType}
        availabilityFrom={availabilityFrom}
        availabilityTo={availabilityTo}
        availability={availability}
        availabilityLoading={availabilityLoading}
        availabilityError={availabilityError}
        onRoomTypeChange={setSelectedRoomTypeId}
        onAvailabilityFromChange={setAvailabilityFrom}
        onAvailabilityToChange={setAvailabilityTo}
        onCheckAvailability={checkAvailability}
      />
    </section>
  );
}

type HotelDetailsPanelProps = {
  selectedHotel: HotelResponse | null;
  loading: boolean;
  error: unknown;
  selectedRoomTypeId: UUID | null;
  selectedRoomType: RoomTypeResponse | null;
  availabilityFrom: string;
  availabilityTo: string;
  availability: RoomAvailabilityResponse[];
  availabilityLoading: boolean;
  availabilityError: unknown;
  onRoomTypeChange: (roomTypeId: UUID) => void;
  onAvailabilityFromChange: (value: string) => void;
  onAvailabilityToChange: (value: string) => void;
  onCheckAvailability: () => void;
};

/**
 * HotelDetailsPanel is intentionally kept as a separate component inside the same file.
 *
 * This is a common React refactoring step:
 * once JSX grows, extract a smaller component with explicit props.
 *
 * In Java terms, props are similar to constructor parameters for a small immutable view object.
 */
function HotelDetailsPanel({
  selectedHotel,
  loading,
  error,
  selectedRoomTypeId,
  selectedRoomType,
  availabilityFrom,
  availabilityTo,
  availability,
  availabilityLoading,
  availabilityError,
  onRoomTypeChange,
  onAvailabilityFromChange,
  onAvailabilityToChange,
  onCheckAvailability,
}: HotelDetailsPanelProps) {
  if (loading) {
    return (
      <div className="details-panel">
        <LoadingState text="Loading hotel details..." />
      </div>
    );
  }

  if (error !== null) {
    return (
      <div className="details-panel">
        <ErrorMessage error={error} />
      </div>
    );
  }

  if (selectedHotel === null) {
    return (
      <div className="details-panel details-panel-empty">
        <p className="eyebrow">Hotel details</p>
        <h2>Select a hotel</h2>
        <p className="muted-text">
          Click Open details on any hotel card to inspect room types and check availability.
        </p>
      </div>
    );
  }

  return (
    <div className="details-panel">
      <div className="details-header">
        <div>
          <p className="eyebrow">{selectedHotel.city}</p>
          <h2>{selectedHotel.name}</h2>
          <p className="muted-text">
            Choose a room type and date range. The next UI step will use these values to create a
            booking saga.
          </p>
        </div>
      </div>

      <div className="details-grid">
        <div className="form-card">
          <h3>Room type</h3>

          {selectedHotel.roomTypes.length === 0 ? (
            <p className="muted-text">This hotel has no room types.</p>
          ) : (
            <div className="room-selector">
              {selectedHotel.roomTypes.map((roomType) => (
                <button
                  className={
                    roomType.roomTypeId === selectedRoomTypeId
                      ? "room-selector-button room-selector-button-active"
                      : "room-selector-button"
                  }
                  key={roomType.roomTypeId}
                  type="button"
                  onClick={() => onRoomTypeChange(roomType.roomTypeId)}
                >
                  <span>{roomType.name}</span>
                  <strong>{roomType.guestCapacity} guests</strong>
                </button>
              ))}
            </div>
          )}

          {selectedRoomType !== null && (
            <div className="technical-id technical-id-compact">
              <span>roomTypeId</span>
              <code>{selectedRoomType.roomTypeId}</code>
            </div>
          )}
        </div>

        <div className="form-card">
          <h3>Availability dates</h3>

          <div className="field-grid">
            <label>
              <span>From</span>
              <input
                type="date"
                value={availabilityFrom}
                onChange={(event) => onAvailabilityFromChange(event.target.value)}
              />
            </label>

            <label>
              <span>To</span>
              <input
                type="date"
                value={availabilityTo}
                onChange={(event) => onAvailabilityToChange(event.target.value)}
              />
            </label>
          </div>

          <button
            className="primary-button primary-button-full"
            type="button"
            disabled={selectedRoomTypeId === null || availabilityLoading}
            onClick={onCheckAvailability}
          >
            {availabilityLoading ? "Checking..." : "Check availability"}
          </button>
        </div>
      </div>

      {availabilityError !== null && <ErrorMessage error={availabilityError} />}

      {availabilityError === null && availability.length > 0 && (
        <AvailabilityTable availability={availability} />
      )}

      {availabilityError === null && !availabilityLoading && availability.length === 0 && (
        <div className="state-card">
          Availability is not loaded yet. Select dates and click Check availability.
        </div>
      )}
    </div>
  );
}

type AvailabilityTableProps = {
  availability: RoomAvailabilityResponse[];
};

function AvailabilityTable({ availability }: AvailabilityTableProps) {
  return (
    <div className="table-card">
      <div className="table-header">
        <h3>Availability</h3>
        <p className="muted-text">
          availableRooms = totalRooms - heldRooms - bookedRooms
        </p>
      </div>

      <div className="responsive-table">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Total</th>
              <th>Held</th>
              <th>Booked</th>
              <th>Available</th>
            </tr>
          </thead>

          <tbody>
            {availability.map((day) => (
              <tr key={day.date}>
                <td>{day.date}</td>
                <td>{day.totalRooms}</td>
                <td>{day.heldRooms}</td>
                <td>{day.bookedRooms}</td>
                <td>
                  <strong className={day.availableRooms > 0 ? "available-positive" : "available-zero"}>
                    {day.availableRooms}
                  </strong>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}