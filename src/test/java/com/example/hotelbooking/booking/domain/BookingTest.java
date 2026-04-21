package com.example.hotelbooking.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

  private static final UUID HOTEL_ID = UUID.randomUUID();
  private static final UUID ROOM_TYPE_ID = UUID.randomUUID();

  @Test
  void shouldCreateNewBooking() {
    Booking booking = createBooking();

    assertNotNull(booking.getId());
    assertEquals(HOTEL_ID, booking.getHotelId());
    assertEquals(ROOM_TYPE_ID, booking.getRoomTypeId());
    assertEquals(2, booking.getGuestCount());
    assertEquals(BookingStatus.NEW, booking.getStatus());
    assertNull(booking.getHoldId());
  }

  @Test
  void shouldPlaceBookingOnHold() {
    Booking booking = createBooking();
    UUID holdId = UUID.randomUUID();

    booking.placeOnHold(holdId);

    assertEquals(BookingStatus.ON_HOLD, booking.getStatus());
    assertEquals(holdId, booking.getHoldId());
  }

  @Test
  void shouldNotPlaceBookingOnHoldWhenStatusIsNotNew() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());

    BookingDomainException ex =
        assertThrows(BookingDomainException.class, () -> booking.placeOnHold(UUID.randomUUID()));

    assertEquals("Only NEW booking can be placed on hold", ex.getMessage());
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
  void shouldNotConfirmBookingWithoutHold() {
    Booking booking = createBooking();

    BookingDomainException ex =
        assertThrows(BookingDomainException.class, booking::confirmHeldBooking);

    assertEquals("Booking cannot be confirmed without an active hold", ex.getMessage());
  }

  @Test
  void shouldNotConfirmAlreadyConfirmedBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.confirmHeldBooking();

    BookingDomainException ex =
        assertThrows(BookingDomainException.class, booking::confirmHeldBooking);

    assertEquals("Booking is already confirmed", ex.getMessage());
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
  void shouldNotCancelBookingWithoutHold() {
    Booking booking = createBooking();

    BookingDomainException ex =
        assertThrows(BookingDomainException.class, booking::cancelHeldBooking);

    assertEquals("Booking has no active hold to release", ex.getMessage());
  }

  @Test
  void shouldNotCancelAlreadyCancelledBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.cancelHeldBooking();

    BookingDomainException ex =
        assertThrows(BookingDomainException.class, booking::cancelHeldBooking);

    assertEquals("Booking is already cancelled", ex.getMessage());
  }

  @Test
  void shouldRejectBooking() {
    Booking booking = createBooking();

    booking.reject();

    assertEquals(BookingStatus.REJECTED, booking.getStatus());
  }

  @Test
  void shouldExpireBooking() {
    Booking booking = createBooking();

    booking.expire();

    assertEquals(BookingStatus.EXPIRED, booking.getStatus());
  }

  @Test
  void shouldNotRejectConfirmedBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.confirmHeldBooking();

    BookingDomainException ex = assertThrows(BookingDomainException.class, booking::reject);

    assertEquals("Rejected status cannot be applied to a final booking", ex.getMessage());
  }

  @Test
  void shouldNotExpireCancelledBooking() {
    Booking booking = createBooking();
    booking.placeOnHold(UUID.randomUUID());
    booking.cancelHeldBooking();

    BookingDomainException ex = assertThrows(BookingDomainException.class, booking::expire);

    assertEquals("Expired status cannot be applied to a final booking", ex.getMessage());
  }

  @Test
  void shouldNotCreateBookingWithNonPositiveGuestCount() {
    StayPeriod stayPeriod =
        new StayPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

    BookingDomainException ex =
        assertThrows(
            BookingDomainException.class,
            () -> Booking.create(HOTEL_ID, ROOM_TYPE_ID, stayPeriod, 0));

    assertEquals("guestCount must be positive", ex.getMessage());
  }

  private Booking createBooking() {
    StayPeriod stayPeriod =
        new StayPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
    return Booking.create(HOTEL_ID, ROOM_TYPE_ID, stayPeriod, 2);
  }
}
