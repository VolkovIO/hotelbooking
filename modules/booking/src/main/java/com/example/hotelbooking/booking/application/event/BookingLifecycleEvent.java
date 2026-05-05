package com.example.hotelbooking.booking.application.event;

import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record BookingLifecycleEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    BookingId bookingId,
    Map<String, Object> payload) {

  private static final int EVENT_VERSION = 1;

  private static final String BOOKING_PLACED_ON_HOLD = "BookingPlacedOnHold";
  private static final String BOOKING_CONFIRMED = "BookingConfirmed";
  private static final String BOOKING_CANCELLED = "BookingCancelled";

  public BookingLifecycleEvent {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(eventType, "eventType must not be null");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    payload = Map.copyOf(Objects.requireNonNull(payload, "payload must not be null"));
  }

  public static BookingLifecycleEvent placedOnHold(Booking booking) {
    return new BookingLifecycleEvent(
        UUID.randomUUID(),
        BOOKING_PLACED_ON_HOLD,
        EVENT_VERSION,
        Instant.now(),
        booking.getId(),
        payload(booking, Map.of("holdId", booking.getHoldId())));
  }

  public static BookingLifecycleEvent confirmed(Booking booking, UUID confirmedHoldId) {
    return new BookingLifecycleEvent(
        UUID.randomUUID(),
        BOOKING_CONFIRMED,
        EVENT_VERSION,
        Instant.now(),
        booking.getId(),
        payload(booking, Map.of("confirmedHoldId", confirmedHoldId)));
  }

  public static BookingLifecycleEvent cancelled(Booking booking, BookingStatus previousStatus) {
    return new BookingLifecycleEvent(
        UUID.randomUUID(),
        BOOKING_CANCELLED,
        EVENT_VERSION,
        Instant.now(),
        booking.getId(),
        payload(booking, Map.of("previousStatus", previousStatus.name())));
  }

  private static Map<String, Object> payload(Booking booking, Map<String, Object> additionalData) {
    Map<String, Object> payload = new LinkedHashMap<>();

    payload.put("bookingId", booking.getId().value());
    payload.put("userId", booking.getUserId().value());
    payload.put("hotelId", booking.getHotelId());
    payload.put("roomTypeId", booking.getRoomTypeId());
    payload.put("checkIn", booking.getStayPeriod().checkIn());
    payload.put("checkOut", booking.getStayPeriod().checkOut());
    payload.put("guestCount", booking.getGuestCount());
    payload.put("status", booking.getStatus().name());

    payload.putAll(additionalData);

    return Map.copyOf(payload);
  }
}
