package com.example.hotelbooking.booking.domain;

import java.util.Objects;
import java.util.UUID;

public final class Booking {

  private final BookingId id;
  private final UUID hotelId;
  private final UUID roomTypeId;
  private final StayPeriod stayPeriod;
  private final int guestCount;

  private BookingStatus status;
  private UUID holdId;

  private Booking(
      BookingId id,
      UUID hotelId,
      UUID roomTypeId,
      StayPeriod stayPeriod,
      int guestCount,
      BookingStatus status,
      UUID holdId) {

    this.id = Objects.requireNonNull(id, "id must not be null");
    this.hotelId = Objects.requireNonNull(hotelId, "hotelId must not be null");
    this.roomTypeId = Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    this.stayPeriod = Objects.requireNonNull(stayPeriod, "stayPeriod must not be null");
    this.guestCount = requirePositive(guestCount, "guestCount must be positive");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.holdId = holdId;
  }

  public static Booking create(
      UUID hotelId, UUID roomTypeId, StayPeriod stayPeriod, int guestCount) {
    return new Booking(
        BookingId.newId(), hotelId, roomTypeId, stayPeriod, guestCount, BookingStatus.NEW, null);
  }

  public void placeOnHold(UUID holdId) {
    ensureStatus(BookingStatus.NEW, "Only NEW booking can be placed on hold");
    this.holdId = Objects.requireNonNull(holdId, "holdId must not be null");
    this.status = BookingStatus.ON_HOLD;
  }

  public void confirm() {
    ensureStatus(BookingStatus.ON_HOLD, "Only ON_HOLD booking can be confirmed");
    status = BookingStatus.CONFIRMED;
  }

  public void reject() {
    ensureNotFinal("Rejected status cannot be applied to a final booking");
    status = BookingStatus.REJECTED;
  }

  public void expire() {
    ensureNotFinal("Expired status cannot be applied to a final booking");
    status = BookingStatus.EXPIRED;
  }

  public void cancel() {
    ensureStatus(BookingStatus.ON_HOLD, "Only ON_HOLD booking can be cancelled");
    status = BookingStatus.CANCELLED;
  }

  public BookingId getId() {
    return id;
  }

  public UUID getHotelId() {
    return hotelId;
  }

  public UUID getRoomTypeId() {
    return roomTypeId;
  }

  public StayPeriod getStayPeriod() {
    return stayPeriod;
  }

  public int getGuestCount() {
    return guestCount;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public UUID getHoldId() {
    return holdId;
  }

  private static int requirePositive(int value, String message) {
    if (value <= 0) {
      throw new BookingDomainException(message);
    }
    return value;
  }

  private void ensureStatus(BookingStatus expected, String message) {
    if (status != expected) {
      throw new BookingDomainException(message);
    }
  }

  private void ensureNotFinal(String message) {
    if (status == BookingStatus.CONFIRMED
        || status == BookingStatus.REJECTED
        || status == BookingStatus.EXPIRED
        || status == BookingStatus.CANCELLED) {
      throw new BookingDomainException(message);
    }
  }
}
