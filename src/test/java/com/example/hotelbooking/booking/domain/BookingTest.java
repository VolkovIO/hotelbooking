package com.example.hotelbooking.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

  @Test
  void shouldCreateBookingInNewStatus() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)),
            2);

    assertEquals(BookingStatus.NEW, booking.getStatus(), "Booking should be created in NEW status");
  }

  @Test
  void shouldPlaceBookingOnHold() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)),
            2);

    booking.placeOnHold(UUID.randomUUID());

    assertEquals(
        BookingStatus.ON_HOLD, booking.getStatus(), "Booking should transition to ON_HOLD status");
  }

  @Test
  void shouldConfirmBookingFromOnHoldStatus() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)),
            2);

    booking.placeOnHold(UUID.randomUUID());
    booking.confirm();

    assertEquals(
        BookingStatus.CONFIRMED,
        booking.getStatus(),
        "Booking should transition to CONFIRMED status from ON_HOLD");
  }

  @Test
  void shouldThrowExceptionWhenConfirmingBookingNotOnHold() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)),
            2);

    assertThrows(
        BookingDomainException.class,
        booking::confirm,
        "Confirming booking outside ON_HOLD status should throw BookingDomainException");
  }

  @Test
  void shouldThrowExceptionWhenStayPeriodIsInvalid() {
    assertThrows(
        BookingDomainException.class,
        () -> new StayPeriod(LocalDate.now().plusDays(3), LocalDate.now().plusDays(1)),
        "StayPeriod with checkOut before checkIn should throw BookingDomainException");
  }
}
