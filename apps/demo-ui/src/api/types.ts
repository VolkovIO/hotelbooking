/**
 * Frontend aliases for backend scalar types.
 *
 * Java UUID, LocalDate and Instant are transferred through JSON as strings.
 * We keep these aliases to make API contracts easier to read.
 */
export type UUID = string;
export type LocalDate = string;
export type Instant = string;

export type BookingStatus =
  | "NEW"
  | "ON_HOLD"
  | "CONFIRMED"
  | "REJECTED"
  | "EXPIRED"
  | "CANCELLED";

export type BookingSagaStatus =
  | "STARTED"
  | "IN_PROGRESS"
  | "WAITING_RETRY"
  | "COMPENSATING"
  | "COMPLETED"
  | "COMPENSATED"
  | "FAILED";

export type BookingSagaStep =
  | "HOLD_INVENTORY"
  | "AUTHORIZE_PAYMENT"
  | "CONFIRM_BOOKING"
  | "APPROVE_PAYMENT"
  | "CANCEL_PAYMENT"
  | "RELEASE_INVENTORY"
  | "CANCEL_BOOKING"
  | "COMPLETE";

/**
 * Mirrors BookingResponse from booking-service.
 */
export type BookingResponse = {
  bookingId: UUID;
  userId: UUID;
  hotelId: UUID;
  roomTypeId: UUID;
  checkIn: LocalDate;
  checkOut: LocalDate;
  guestCount: number;
  status: BookingStatus;
};

/**
 * Mirrors BookingPageResponse from booking-service.
 *
 * This is the response for:
 * GET /api/v1/bookings/my?page=0&size=20
 */
export type BookingPageResponse = {
  content: BookingResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

/**
 * Mirrors StartBookingSagaRequest from booking-service.
 *
 * BigDecimal is sent as JSON number here because this is a demo UI.
 * For financial production systems we would be stricter and represent money
 * as minor units or decimal strings.
 */
export type StartBookingSagaRequest = {
  hotelId: UUID;
  roomTypeId: UUID;
  checkIn: LocalDate;
  checkOut: LocalDate;
  guestCount: number;
  paymentAmount: number;
  paymentCurrency: string;
};

/**
 * Mirrors BookingSagaResponse from booking-service.
 */
export type BookingSagaResponse = {
  sagaId: UUID;
  bookingId: UUID;
  sagaStatus: BookingSagaStatus;
  currentStep: BookingSagaStep;
  paymentId: UUID | null;
  retryCount: number;
  lastFailureReason: string | null;
};

/**
 * Mirrors HotelSummaryResponse from inventory-service.
 */
export type HotelSummaryResponse = {
  hotelId: UUID;
  name: string;
  city: string;
  roomTypes: RoomTypeSummaryResponse[];
};

/**
 * Mirrors RoomTypeSummaryResponse from inventory-service.
 */
export type RoomTypeSummaryResponse = {
  roomTypeId: UUID;
  name: string;
  guestCapacity: number;
};

/**
 * Mirrors HotelResponse from inventory-service.
 */
export type HotelResponse = {
  hotelId: UUID;
  name: string;
  city: string;
  roomTypes: RoomTypeResponse[];
};

/**
 * Mirrors nested HotelResponse.RoomTypeResponse from inventory-service.
 */
export type RoomTypeResponse = {
  roomTypeId: UUID;
  name: string;
  guestCapacity: number;
};

/**
 * Mirrors RoomAvailabilityResponse from inventory-service.
 */
export type RoomAvailabilityResponse = {
  date: LocalDate;
  totalRooms: number;
  heldRooms: number;
  bookedRooms: number;
  availableRooms: number;
};

/**
 * Mirrors RegisterHotelRequest from inventory-service.
 */
export type RegisterHotelRequest = {
  name: string;
  city: string;
};

/**
 * Mirrors AddRoomTypeRequest from inventory-service.
 */
export type AddRoomTypeRequest = {
  name: string;
  guestCapacity: number;
};

/**
 * Mirrors SetRoomAvailabilityRequest from inventory-service.
 */
export type SetRoomAvailabilityRequest = {
  from: LocalDate;
  to: LocalDate;
  totalRooms: number;
};

/**
 * Mirrors TimelineEventResponse from audit-service.
 *
 * payload is intentionally typed as Record<string, unknown> because different
 * event types have different payload structures.
 */
export type TimelineEventResponse = {
  eventId: UUID;
  eventType: string;
  eventVersion: number;
  source: string;
  aggregateType: string;
  aggregateId: UUID;
  bookingId: UUID;
  occurredAt: Instant;
  correlationId: UUID | null;
  causationId: UUID | null;
  payload: Record<string, unknown>;
  recordedAt: Instant;
};