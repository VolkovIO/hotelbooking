import { useEffect, useMemo, useState } from "react";
import { inventoryApi, type HotelSummaryResponse, type UUID } from "../api";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingState } from "../components/LoadingState";
import { TechnicalId } from "../components/TechnicalId";

const DEFAULT_AVAILABILITY_FROM = "2030-06-11";
const DEFAULT_AVAILABILITY_TO = "2030-06-20";
const DEFAULT_TOTAL_ROOMS = 10;

/**
 * InventoryAdminPage is a lightweight admin console for the demo.
 *
 * It intentionally does not try to become a full admin product.
 * Its goal is practical:
 *
 * 1. create hotel
 * 2. add room type
 * 3. initialize availability
 * 4. go to Hotels page and book it
 */
export function InventoryAdminPage() {
  const [hotels, setHotels] = useState<HotelSummaryResponse[]>([]);
  const [hotelsLoading, setHotelsLoading] = useState(true);
  const [hotelsError, setHotelsError] = useState<unknown>(null);

  const [selectedHotelId, setSelectedHotelId] = useState<UUID | null>(null);
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<UUID | null>(null);

  const [hotelName, setHotelName] = useState("Demo Hotel");
  const [hotelCity, setHotelCity] = useState("Kazan");
  const [createHotelLoading, setCreateHotelLoading] = useState(false);
  const [createHotelError, setCreateHotelError] = useState<unknown>(null);
  const [createHotelMessage, setCreateHotelMessage] = useState<string | null>(null);

  const [roomTypeName, setRoomTypeName] = useState("Standard Room");
  const [guestCapacity, setGuestCapacity] = useState(2);
  const [addRoomTypeLoading, setAddRoomTypeLoading] = useState(false);
  const [addRoomTypeError, setAddRoomTypeError] = useState<unknown>(null);
  const [addRoomTypeMessage, setAddRoomTypeMessage] = useState<string | null>(null);

  const [availabilityFrom, setAvailabilityFrom] = useState(DEFAULT_AVAILABILITY_FROM);
  const [availabilityTo, setAvailabilityTo] = useState(DEFAULT_AVAILABILITY_TO);
  const [totalRooms, setTotalRooms] = useState(DEFAULT_TOTAL_ROOMS);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [availabilityError, setAvailabilityError] = useState<unknown>(null);
  const [availabilityMessage, setAvailabilityMessage] = useState<string | null>(null);

  const selectedHotel = useMemo(
    () => hotels.find((hotel) => hotel.hotelId === selectedHotelId) ?? null,
    [hotels, selectedHotelId],
  );

  const selectedRoomType = useMemo(
    () =>
      selectedHotel?.roomTypes.find((roomType) => roomType.roomTypeId === selectedRoomTypeId) ??
      null,
    [selectedHotel, selectedRoomTypeId],
  );

  useEffect(() => {
    loadHotels();
  }, []);

  async function loadHotels() {
    try {
      setHotelsLoading(true);
      setHotelsError(null);

      const result = await inventoryApi.findHotels(50);

      setHotels(result);

      if (result.length > 0) {
        setSelectedHotelId((currentSelectedHotelId) => {
          const currentStillExists = result.some(
            (hotel) => hotel.hotelId === currentSelectedHotelId,
          );

          return currentStillExists ? currentSelectedHotelId : result[0].hotelId;
        });
      }
    } catch (requestError) {
      setHotelsError(requestError);
    } finally {
      setHotelsLoading(false);
    }
  }

  async function createHotel() {
    try {
      setCreateHotelLoading(true);
      setCreateHotelError(null);
      setCreateHotelMessage(null);

      const createdHotel = await inventoryApi.registerHotel({
        name: hotelName.trim(),
        city: hotelCity.trim(),
      });

      setCreateHotelMessage(`Hotel ${createdHotel.name} created.`);
      setSelectedHotelId(createdHotel.hotelId);
      setSelectedRoomTypeId(null);
      setHotelName("Demo Hotel");
      setHotelCity("Kazan");

      await loadHotels();
    } catch (requestError) {
      setCreateHotelError(requestError);
    } finally {
      setCreateHotelLoading(false);
    }
  }

  async function addRoomType() {
    if (selectedHotelId === null) {
      return;
    }

    try {
      setAddRoomTypeLoading(true);
      setAddRoomTypeError(null);
      setAddRoomTypeMessage(null);

      const updatedHotel = await inventoryApi.addRoomType(selectedHotelId, {
        name: roomTypeName.trim(),
        guestCapacity,
      });

      const lastRoomType = updatedHotel.roomTypes[updatedHotel.roomTypes.length - 1] ?? null;

      setAddRoomTypeMessage(`Room type ${roomTypeName.trim()} added.`);
      setSelectedRoomTypeId(lastRoomType?.roomTypeId ?? null);
      setRoomTypeName("Standard Room");
      setGuestCapacity(2);

      await loadHotels();
    } catch (requestError) {
      setAddRoomTypeError(requestError);
    } finally {
      setAddRoomTypeLoading(false);
    }
  }

  async function initializeAvailability() {
    if (selectedHotelId === null || selectedRoomTypeId === null) {
      return;
    }

    try {
      setAvailabilityLoading(true);
      setAvailabilityError(null);
      setAvailabilityMessage(null);

      await inventoryApi.initializeRoomAvailability(selectedHotelId, selectedRoomTypeId, {
        from: availabilityFrom,
        to: availabilityTo,
        totalRooms,
      });

      setAvailabilityMessage(
        `Availability initialized from ${availabilityFrom} to ${availabilityTo}.`,
      );
    } catch (requestError) {
      setAvailabilityError(requestError);
    } finally {
      setAvailabilityLoading(false);
    }
  }

  const canCreateHotel = hotelName.trim().length > 0 && hotelCity.trim().length > 0;

  const canAddRoomType =
    selectedHotelId !== null && roomTypeName.trim().length > 0 && guestCapacity > 0;

  const canInitializeAvailability =
    selectedHotelId !== null &&
    selectedRoomTypeId !== null &&
    availabilityFrom.length > 0 &&
    availabilityTo.length > 0 &&
    totalRooms > 0;

  return (
    <section className="page-section">
      <div className="page-header">
        <div>
          <p className="eyebrow">Inventory service admin</p>
          <h1>Inventory Admin</h1>
          <p className="page-description">
            Minimal admin console for demo data: create hotels, add room types and initialize
            availability for booking scenarios.
          </p>
        </div>

        <button className="secondary-button secondary-button-strong" type="button" onClick={loadHotels}>
          Refresh catalog
        </button>
      </div>

      <div className="admin-layout">
        <div className="admin-main">
          <AdminCard
            title="1. Create hotel"
            description="Register a hotel in inventory-service. After creation it can be selected below."
          >
            {createHotelMessage !== null && (
              <div className="mini-feedback mini-feedback-success">{createHotelMessage}</div>
            )}

            {createHotelError !== null && <ErrorMessage error={createHotelError} />}

            <div className="admin-form-grid">
              <label>
                <span>Hotel name</span>
                <input
                  value={hotelName}
                  onChange={(event) => setHotelName(event.target.value)}
                />
              </label>

              <label>
                <span>City</span>
                <input value={hotelCity} onChange={(event) => setHotelCity(event.target.value)} />
              </label>
            </div>

            <button
              className="primary-button primary-button-full"
              type="button"
              disabled={!canCreateHotel || createHotelLoading}
              onClick={createHotel}
            >
              {createHotelLoading ? "Creating hotel..." : "Create hotel"}
            </button>
          </AdminCard>

          <AdminCard
            title="2. Add room type"
            description="Choose a hotel and add a room type with guest capacity."
          >
            {addRoomTypeMessage !== null && (
              <div className="mini-feedback mini-feedback-success">{addRoomTypeMessage}</div>
            )}

            {addRoomTypeError !== null && <ErrorMessage error={addRoomTypeError} />}

            <HotelSelector
              hotels={hotels}
              selectedHotelId={selectedHotelId}
              onHotelChange={(hotelId) => {
                setSelectedHotelId(hotelId);
                const nextHotel = hotels.find((hotel) => hotel.hotelId === hotelId) ?? null;
                setSelectedRoomTypeId(nextHotel?.roomTypes[0]?.roomTypeId ?? null);
              }}
            />

            <div className="admin-form-grid">
              <label>
                <span>Room type name</span>
                <input
                  value={roomTypeName}
                  onChange={(event) => setRoomTypeName(event.target.value)}
                />
              </label>

              <label>
                <span>Guest capacity</span>
                <input
                  min={1}
                  type="number"
                  value={guestCapacity}
                  onChange={(event) =>
                    setGuestCapacity(toPositiveNumber(event.target.value, 2))
                  }
                />
              </label>
            </div>

            <button
              className="primary-button primary-button-full"
              type="button"
              disabled={!canAddRoomType || addRoomTypeLoading}
              onClick={addRoomType}
            >
              {addRoomTypeLoading ? "Adding room type..." : "Add room type"}
            </button>
          </AdminCard>

          <AdminCard
            title="3. Initialize availability"
            description="Select hotel and room type, then create daily room capacity for a date range."
          >
            {availabilityMessage !== null && (
              <div className="mini-feedback mini-feedback-success">{availabilityMessage}</div>
            )}

            {availabilityError !== null && <ErrorMessage error={availabilityError} />}

            <HotelSelector
              hotels={hotels}
              selectedHotelId={selectedHotelId}
              onHotelChange={(hotelId) => {
                setSelectedHotelId(hotelId);
                const nextHotel = hotels.find((hotel) => hotel.hotelId === hotelId) ?? null;
                setSelectedRoomTypeId(nextHotel?.roomTypes[0]?.roomTypeId ?? null);
              }}
            />

            <RoomTypeSelector
              hotel={selectedHotel}
              selectedRoomTypeId={selectedRoomTypeId}
              onRoomTypeChange={setSelectedRoomTypeId}
            />

            <div className="admin-form-grid admin-form-grid-three">
              <label>
                <span>From</span>
                <input
                  type="date"
                  value={availabilityFrom}
                  onChange={(event) => setAvailabilityFrom(event.target.value)}
                />
              </label>

              <label>
                <span>To</span>
                <input
                  type="date"
                  value={availabilityTo}
                  onChange={(event) => setAvailabilityTo(event.target.value)}
                />
              </label>

              <label>
                <span>Total rooms</span>
                <input
                  min={1}
                  type="number"
                  value={totalRooms}
                  onChange={(event) => setTotalRooms(toPositiveNumber(event.target.value, 10))}
                />
              </label>
            </div>

            <button
              className="primary-button primary-button-full"
              type="button"
              disabled={!canInitializeAvailability || availabilityLoading}
              onClick={initializeAvailability}
            >
              {availabilityLoading ? "Initializing availability..." : "Initialize availability"}
            </button>
          </AdminCard>
        </div>

        <aside className="admin-sidebar">
          <div className="admin-summary-card">
            <p className="eyebrow">Selected inventory</p>

            {selectedHotel === null ? (
              <p className="muted-text">Select or create a hotel.</p>
            ) : (
              <>
                <h2>{selectedHotel.name}</h2>
                <p className="muted-text">{selectedHotel.city}</p>

                <div className="admin-selected-id">
                  <span>hotelId</span>
                  <TechnicalId value={selectedHotel.hotelId} />
                </div>

                <div className="admin-room-list">
                  {selectedHotel.roomTypes.length === 0 ? (
                    <p className="muted-text">No room types yet.</p>
                  ) : (
                    selectedHotel.roomTypes.map((roomType) => (
                      <button
                        className={
                          roomType.roomTypeId === selectedRoomTypeId
                            ? "admin-room-button admin-room-button-active"
                            : "admin-room-button"
                        }
                        key={roomType.roomTypeId}
                        type="button"
                        onClick={() => setSelectedRoomTypeId(roomType.roomTypeId)}
                      >
                        <span>{roomType.name}</span>
                        <strong>{roomType.guestCapacity} guests</strong>
                      </button>
                    ))
                  )}
                </div>

                {selectedRoomType !== null && (
                  <div className="admin-selected-id">
                    <span>roomTypeId</span>
                    <TechnicalId value={selectedRoomType.roomTypeId} />
                  </div>
                )}
              </>
            )}
          </div>

          <div className="admin-hint-card">
            <strong>Demo path</strong>
            <ol>
              <li>Create hotel</li>
              <li>Add room type</li>
              <li>Initialize availability</li>
              <li>Open Hotels tab</li>
              <li>Check availability and start saga</li>
            </ol>
          </div>
        </aside>
      </div>

      {hotelsLoading && <LoadingState text="Loading inventory catalog..." />}

      {!hotelsLoading && hotelsError !== null && <ErrorMessage error={hotelsError} />}
    </section>
  );
}

type AdminCardProps = {
  title: string;
  description: string;
  children: React.ReactNode;
};

function AdminCard({ title, description, children }: AdminCardProps) {
  return (
    <section className="admin-card">
      <div className="admin-card-header">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>

      {children}
    </section>
  );
}

type HotelSelectorProps = {
  hotels: HotelSummaryResponse[];
  selectedHotelId: UUID | null;
  onHotelChange: (hotelId: UUID) => void;
};

function HotelSelector({ hotels, selectedHotelId, onHotelChange }: HotelSelectorProps) {
  return (
    <label className="admin-select-field">
      <span>Hotel</span>
      <select
        value={selectedHotelId ?? ""}
        onChange={(event) => onHotelChange(event.target.value)}
      >
        <option value="" disabled>
          Select hotel
        </option>

        {hotels.map((hotel) => (
          <option key={hotel.hotelId} value={hotel.hotelId}>
            {hotel.name} — {hotel.city}
          </option>
        ))}
      </select>
    </label>
  );
}

type RoomTypeSelectorProps = {
  hotel: HotelSummaryResponse | null;
  selectedRoomTypeId: UUID | null;
  onRoomTypeChange: (roomTypeId: UUID) => void;
};

function RoomTypeSelector({ hotel, selectedRoomTypeId, onRoomTypeChange }: RoomTypeSelectorProps) {
  return (
    <label className="admin-select-field">
      <span>Room type</span>
      <select
        value={selectedRoomTypeId ?? ""}
        disabled={hotel === null || hotel.roomTypes.length === 0}
        onChange={(event) => onRoomTypeChange(event.target.value)}
      >
        <option value="" disabled>
          Select room type
        </option>

        {hotel?.roomTypes.map((roomType) => (
          <option key={roomType.roomTypeId} value={roomType.roomTypeId}>
            {roomType.name} — {roomType.guestCapacity} guests
          </option>
        ))}
      </select>
    </label>
  );
}

function toPositiveNumber(value: string, fallback: number): number {
  const parsed = Number(value);

  if (Number.isNaN(parsed) || parsed <= 0) {
    return fallback;
  }

  return parsed;
}