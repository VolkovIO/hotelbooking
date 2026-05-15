import { useEffect, useMemo, useState } from "react";
import {
  bookingApi,
  inventoryApi,
  type BookingSagaResponse,
  type HotelResponse,
  type HotelSummaryResponse,
  type RoomAvailabilityResponse,
  type RoomTypeResponse,
  type SagaEngine,
  type UUID,
} from "../api";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingState } from "../components/LoadingState";
import { TechnicalId } from "../components/TechnicalId";

/**
 * Default demo dates.
 *
 * They match our local demo data style where availability is usually created
 * for future dates such as 2030-06-11.
 */
const DEFAULT_FROM = "2030-06-11";
const DEFAULT_TO = "2030-06-12";

/**
 * Default payment amount for the happy path.
 *
 * In our fake payment provider:
 * - amount <= 50000 usually succeeds
 * - amount > 50000 demonstrates payment decline / compensation flow
 */
const DEFAULT_PAYMENT_AMOUNT = 1500;

/**
 * HotelsPage currently contains the whole first demo flow:
 *
 * 1. Load hotels from inventory-service.
 * 2. Open hotel details.
 * 3. Select room type.
 * 4. Check availability.
 * 5. Start booking saga.
 *
 * This is intentionally kept in one file for the first UI iteration.
 * Later, after the flow is stable, we can split it into smaller pages/components.
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

  const [guestCount, setGuestCount] = useState(1);
  const [paymentAmount, setPaymentAmount] = useState(DEFAULT_PAYMENT_AMOUNT);
  const [paymentCurrency, setPaymentCurrency] = useState("RUB");
  const [sagaEngine, setSagaEngine] = useState<SagaEngine>("handmade");
  const [bookingSagaResult, setBookingSagaResult] = useState<BookingSagaResponse | null>(null);
  const [bookingSagaLoading, setBookingSagaLoading] = useState(false);
  const [bookingSagaError, setBookingSagaError] = useState<unknown>(null);

  const selectedRoomType = useMemo(
    () =>
      selectedHotel?.roomTypes.find((roomType) => roomType.roomTypeId === selectedRoomTypeId) ??
      null,
    [selectedHotel, selectedRoomTypeId],
  );

  /**
   * A very small client-side check.
   *
   * The backend is still the source of truth. This check only helps the demo UI
   * avoid sending obviously incomplete requests.
   */
  const canStartBookingSaga =
    selectedHotel !== null &&
    selectedRoomTypeId !== null &&
    availabilityFrom.length > 0 &&
    availabilityTo.length > 0 &&
    guestCount > 0 &&
    paymentAmount > 0 &&
    paymentCurrency.trim().length > 0;

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
      setBookingSagaResult(null);
      setHotelDetailsLoading(true);
      setHotelDetailsError(null);
      setAvailabilityError(null);
      setBookingSagaError(null);

      const hotel = await inventoryApi.getHotelById(hotelId);

      setSelectedHotel(hotel);
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

  async function startBookingSaga() {
    if (selectedHotel === null || selectedRoomTypeId === null) {
      return;
    }

    try {
      setBookingSagaLoading(true);
      setBookingSagaError(null);
      setBookingSagaResult(null);

      const result = await bookingApi.startBookingSaga(
        {
          hotelId: selectedHotel.hotelId,
          roomTypeId: selectedRoomTypeId,
          checkIn: availabilityFrom,
          checkOut: availabilityTo,
          guestCount,
          paymentAmount,
          paymentCurrency: paymentCurrency.trim().toUpperCase(),
        },
        sagaEngine,
      );

      setBookingSagaResult(result);
    } catch (requestError) {
      setBookingSagaError(requestError);
    } finally {
      setBookingSagaLoading(false);
    }
  }

  return (
    <section className="page-section">
      <div className="page-header">
        <div>
          <p className="eyebrow">Inventory catalog</p>
          <h1>Hotels</h1>
          <p className="page-description">
            Select a hotel, inspect its room types, check availability and start
            the booking saga.
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
        guestCount={guestCount}
        paymentAmount={paymentAmount}
        paymentCurrency={paymentCurrency}
        sagaEngine={sagaEngine}
        canStartBookingSaga={canStartBookingSaga}
        bookingSagaLoading={bookingSagaLoading}
        bookingSagaError={bookingSagaError}
        bookingSagaResult={bookingSagaResult}
        onRoomTypeChange={setSelectedRoomTypeId}
        onAvailabilityFromChange={setAvailabilityFrom}
        onAvailabilityToChange={setAvailabilityTo}
        onGuestCountChange={setGuestCount}
        onPaymentAmountChange={setPaymentAmount}
        onPaymentCurrencyChange={setPaymentCurrency}
        onSagaEngineChange={setSagaEngine}
        onCheckAvailability={checkAvailability}
        onStartBookingSaga={startBookingSaga}
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
  guestCount: number;
  paymentAmount: number;
  paymentCurrency: string;
  sagaEngine: SagaEngine;
  canStartBookingSaga: boolean;
  bookingSagaLoading: boolean;
  bookingSagaError: unknown;
  bookingSagaResult: BookingSagaResponse | null;
  onRoomTypeChange: (roomTypeId: UUID) => void;
  onAvailabilityFromChange: (value: string) => void;
  onAvailabilityToChange: (value: string) => void;
  onGuestCountChange: (value: number) => void;
  onPaymentAmountChange: (value: number) => void;
  onPaymentCurrencyChange: (value: string) => void;
  onSagaEngineChange: (value: SagaEngine) => void;
  onCheckAvailability: () => void;
  onStartBookingSaga: () => void;
};

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
  guestCount,
  paymentAmount,
  paymentCurrency,
  sagaEngine,
  canStartBookingSaga,
  bookingSagaLoading,
  bookingSagaError,
  bookingSagaResult,
  onRoomTypeChange,
  onAvailabilityFromChange,
  onAvailabilityToChange,
  onGuestCountChange,
  onPaymentAmountChange,
  onPaymentCurrencyChange,
  onSagaEngineChange,
  onCheckAvailability,
  onStartBookingSaga,
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
          Click Open details on any hotel card to inspect room types, check availability and create
          a booking through saga.
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
            Choose a room type and date range. Then start the booking saga to reserve inventory,
            authorize payment and confirm booking.
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

      <BookingSagaForm
        guestCount={guestCount}
        paymentAmount={paymentAmount}
        paymentCurrency={paymentCurrency}
        sagaEngine={sagaEngine}
        canStartBookingSaga={canStartBookingSaga}
        loading={bookingSagaLoading}
        error={bookingSagaError}
        result={bookingSagaResult}
        onGuestCountChange={onGuestCountChange}
        onPaymentAmountChange={onPaymentAmountChange}
        onPaymentCurrencyChange={onPaymentCurrencyChange}
        onSagaEngineChange={onSagaEngineChange}
        onStartBookingSaga={onStartBookingSaga}
      />
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
        <p className="muted-text">availableRooms = totalRooms - heldRooms - bookedRooms</p>
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

type BookingSagaFormProps = {
  guestCount: number;
  paymentAmount: number;
  paymentCurrency: string;
  sagaEngine: SagaEngine;
  canStartBookingSaga: boolean;
  loading: boolean;
  error: unknown;
  result: BookingSagaResponse | null;
  onGuestCountChange: (value: number) => void;
  onPaymentAmountChange: (value: number) => void;
  onPaymentCurrencyChange: (value: string) => void;
  onSagaEngineChange: (value: SagaEngine) => void;
  onStartBookingSaga: () => void;
};

function BookingSagaForm({
  guestCount,
  paymentAmount,
  paymentCurrency,
  sagaEngine,
  canStartBookingSaga,
  loading,
  error,
  result,
  onGuestCountChange,
  onPaymentAmountChange,
  onPaymentCurrencyChange,
  onSagaEngineChange,
  onStartBookingSaga,
}: BookingSagaFormProps) {
  return (
    <div className="booking-saga-panel">
      <div className="booking-saga-header">
        <div>
          <p className="eyebrow">Booking saga</p>
          <h3>Create booking</h3>
          <p className="muted-text">
            This calls booking-service and starts the distributed flow:
            inventory hold → payment authorization → booking confirmation.
          </p>
        </div>

        <div className="demo-hint">
          <strong>Demo rule</strong>
          <span>paymentAmount &gt; 50000 demonstrates payment decline.</span>
        </div>
      </div>

      <div className="booking-form-grid">
        <label>
          <span>Guests</span>
          <input
            min={1}
            type="number"
            value={guestCount}
            onChange={(event) => onGuestCountChange(toPositiveNumber(event.target.value, 1))}
          />
        </label>

        <label>
          <span>Payment amount</span>
          <input
            min={1}
            type="number"
            value={paymentAmount}
            onChange={(event) =>
              onPaymentAmountChange(toPositiveNumber(event.target.value, DEFAULT_PAYMENT_AMOUNT))
            }
          />
        </label>

        <label>
          <span>Currency</span>
          <input
            maxLength={3}
            value={paymentCurrency}
            onChange={(event) => onPaymentCurrencyChange(event.target.value)}
          />
        </label>

        <label>
          <span>Saga engine</span>
          <select
            value={sagaEngine}
            onChange={(event) => onSagaEngineChange(event.target.value as SagaEngine)}
          >
            <option value="handmade">Handmade saga</option>
            <option value="spring-statemachine">Spring Statemachine prototype</option>
          </select>
        </label>
      </div>

      <button
        className="primary-button primary-button-full"
        type="button"
        disabled={!canStartBookingSaga || loading}
        onClick={onStartBookingSaga}
      >
        {loading ? "Starting booking saga..." : "Start booking saga"}
      </button>

      {error !== null && <ErrorMessage error={error} />}

      {result !== null && <BookingSagaResult result={result} />}
    </div>
  );
}

type BookingSagaResultProps = {
  result: BookingSagaResponse;
};

function BookingSagaResult({ result }: BookingSagaResultProps) {
  return (
    <div className="booking-result-card">
      <div className="booking-result-header">
        <div>
          <p className="eyebrow">Saga started</p>
          <h3>Booking flow accepted</h3>
        </div>

        <span className="saga-status-badge">{result.sagaStatus}</span>
      </div>

      <div className="result-grid">
        <ResultItem label="sagaId" value={result.sagaId} />
        <ResultItem label="bookingId" value={result.bookingId} />
        <ResultItem label="currentStep" value={result.currentStep} />
        <ResultItem label="paymentId" value={result.paymentId ?? "not assigned yet"} />
        <ResultItem label="retryCount" value={String(result.retryCount)} />
        <ResultItem label="lastFailureReason" value={result.lastFailureReason ?? "none"} />
      </div>

      <p className="muted-text">
        The saga may continue asynchronously. Later we will add My bookings and Audit timeline
        screens to observe status changes.
      </p>
    </div>
  );
}

type ResultItemProps = {
  label: string;
  value: string;
};

function ResultItem({ label, value }: ResultItemProps) {
  const looksLikeTechnicalId = value.includes("-") && value.length >= 32;

  return (
    <div className="result-item">
      <span>{label}</span>

      {looksLikeTechnicalId ? (
        <TechnicalId value={value} />
      ) : (
        <code className="result-value">{value}</code>
      )}
    </div>
  );
}

function toPositiveNumber(value: string, fallback: number): number {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed <= 0) {
    return fallback;
  }

  return parsed;
}