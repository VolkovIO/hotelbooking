package com.example.hotelbooking.booking.domain;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NullAssignment"})
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

  public static Booking restore(
      BookingId id,
      UUID hotelId,
      UUID roomTypeId,
      StayPeriod stayPeriod,
      int guestCount,
      BookingStatus status,
      UUID holdId) {

    validateRestoredHoldState(status, holdId);

    return new Booking(id, hotelId, roomTypeId, stayPeriod, guestCount, status, holdId);
  }

  public void placeOnHold(UUID holdId) {
    ensureStatus(BookingStatus.NEW, "Only NEW booking can be placed on hold");
    this.holdId = Objects.requireNonNull(holdId, "holdId must not be null");
    this.status = BookingStatus.ON_HOLD;
  }

  public void confirmHeldBooking() {
    ensureStatus(BookingStatus.ON_HOLD, "Only ON_HOLD booking can be confirmed");
    ensureHoldPresent("Booking cannot be confirmed without an active hold");

    this.status = BookingStatus.CONFIRMED;
    this.holdId = null;
  }

  public void reject() {
    ensureStatusOneOf(
        BookingStatus.NEW, BookingStatus.ON_HOLD, "Only NEW or ON_HOLD booking can be rejected");

    this.status = BookingStatus.REJECTED;
    this.holdId = null;
  }

  public void expire() {
    ensureStatusOneOf(
        BookingStatus.NEW, BookingStatus.ON_HOLD, "Only NEW or ON_HOLD booking can be expired");

    this.status = BookingStatus.EXPIRED;
    this.holdId = null;
  }

  public boolean isOnHold() {
    return status == BookingStatus.ON_HOLD;
  }

  public boolean isConfirmed() {
    return status == BookingStatus.CONFIRMED;
  }

  public void cancelConfirmedBooking() {
    ensureStatus(BookingStatus.CONFIRMED, "Only CONFIRMED booking can be cancelled");

    this.status = BookingStatus.CANCELLED;
    this.holdId = null;
  }

  public void cancelHeldBooking() {
    ensureStatus(BookingStatus.ON_HOLD, "Only ON_HOLD booking can be cancelled");
    ensureHoldPresent("Booking has no active hold to release");

    this.status = BookingStatus.CANCELLED;
    this.holdId = null;
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

  private void ensureStatusOneOf(BookingStatus first, BookingStatus second, String message) {
    if (status != first && status != second) {
      throw new BookingDomainException(message);
    }
  }

  private void ensureHoldPresent(String message) {
    if (holdId == null) {
      throw new BookingDomainException(message);
    }
  }

  private static void validateRestoredHoldState(BookingStatus status, UUID holdId) {
    Objects.requireNonNull(status, "status must not be null");

    if (status == BookingStatus.ON_HOLD && holdId == null) {
      throw new BookingDomainException("ON_HOLD booking must have an active hold");
    }

    if (status != BookingStatus.ON_HOLD && holdId != null) {
      throw new BookingDomainException("Only ON_HOLD booking can have an active hold");
    }
  }
}
