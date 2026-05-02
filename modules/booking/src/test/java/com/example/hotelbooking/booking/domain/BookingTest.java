package com.example.hotelbooking.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.CyclomaticComplexity")
class BookingTest {

  @Test
  void shouldCreateBookingInNewStatus() {
    Booking booking = createBooking();

    assertNotNull(booking.getId());
    assertEquals(BookingStatus.NEW, booking.getStatus());
    assertNull(booking.getHoldId());
    assertEquals(2, booking.getGuestCount());
  }

  @Test
  void shouldPlaceBookingOnHoldFromNewStatus() {
    Booking booking = createBooking();
    UUID holdId = UUID.randomUUID();

    booking.placeOnHold(holdId);

    assertEquals(BookingStatus.ON_HOLD, booking.getStatus());
    assertEquals(holdId, booking.getHoldId());
  }

  @Test
  void shouldConfirmHeldBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());

    booking.confirmHeldBooking();

    assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldCancelHeldBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());

    booking.cancelHeldBooking();

    assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldRejectNewBooking() {
    Booking booking = createBooking();

    booking.reject();

    assertEquals(BookingStatus.REJECTED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldRejectOnHoldBookingAndClearHoldId() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());

    booking.reject();

    assertEquals(BookingStatus.REJECTED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldExpireNewBooking() {
    Booking booking = createBooking();

    booking.expire();

    assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldExpireOnHoldBookingAndClearHoldId() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());

    booking.expire();

    assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldNotPlaceOnHoldWhenStatusIsNotNew() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());

    BookingDomainException exception =
        assertThrows(BookingDomainException.class, () -> booking.placeOnHold(UUID.randomUUID()));

    assertEquals("Only NEW booking can be placed on hold", exception.getMessage());
  }

  @Test
  void shouldNotConfirmBookingWhenStatusIsNotOnHold() {
    Booking booking = createBooking();

    BookingDomainException exception =
        assertThrows(BookingDomainException.class, booking::confirmHeldBooking);

    assertEquals("Only ON_HOLD booking can be confirmed", exception.getMessage());
  }

  @Test
  void shouldNotCancelBookingWhenStatusIsNotOnHold() {
    Booking booking = createBooking();

    BookingDomainException exception =
        assertThrows(BookingDomainException.class, booking::cancelHeldBooking);

    assertEquals("Only ON_HOLD booking can be cancelled", exception.getMessage());
  }

  @Test
  void shouldNotRejectConfirmedBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.confirmHeldBooking();

    BookingDomainException exception = assertThrows(BookingDomainException.class, booking::reject);

    assertEquals("Only NEW or ON_HOLD booking can be rejected", exception.getMessage());
  }

  @Test
  void shouldNotExpireCancelledBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.cancelHeldBooking();

    BookingDomainException exception = assertThrows(BookingDomainException.class, booking::expire);

    assertEquals("Only NEW or ON_HOLD booking can be expired", exception.getMessage());
  }

  @Test
  void shouldRequireHoldIdWhenPlacingOnHold() {
    Booking booking = createBooking();

    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> booking.placeOnHold(null));

    assertEquals("holdId must not be null", exception.getMessage());
  }

  @Test
  void shouldCancelConfirmedBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.confirmHeldBooking();

    booking.cancelConfirmedBooking();

    assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldNotCancelConfirmedBookingWhenStatusIsNotConfirmed() {
    Booking booking = createBooking();

    assertThrows(BookingDomainException.class, booking::cancelConfirmedBooking);
  }

  @Test
  void shouldRestoreConfirmedBooking() {
    Booking booking =
        Booking.restore(
            BookingId.newId(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 12)),
            2,
            BookingStatus.CONFIRMED,
            null);

    assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldRejectRestoringOnHoldBookingWithoutHold() {
    assertThrows(BookingDomainException.class, () -> restoreBooking(BookingStatus.ON_HOLD, null));
  }

  @Test
  void shouldRejectRestoringConfirmedBookingWithHold() {
    assertThrows(
        BookingDomainException.class,
        () -> restoreBooking(BookingStatus.CONFIRMED, UUID.randomUUID()));
  }

  @Test
  void shouldRestoreOnHoldBookingWithHold() {
    UUID holdId = UUID.randomUUID();

    Booking booking = restoreBooking(BookingStatus.ON_HOLD, holdId);

    assertEquals(BookingStatus.ON_HOLD, booking.getStatus());
    assertEquals(holdId, booking.getHoldId());
  }

  @Test
  void shouldRestoreConfirmedBookingWithoutHold() {
    Booking booking = restoreBooking(BookingStatus.CONFIRMED, null);

    assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  private Booking restoreBooking(BookingStatus status, UUID holdId) {
    return Booking.restore(
        BookingId.newId(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 12)),
        2,
        status,
        holdId);
  }

  private Booking createBooking() {
    return Booking.create(
        UUID.randomUUID(),
        UUID.randomUUID(),
        new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20)),
        2);
  }
}
