package com.example.hotelbooking.booking.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.booking.domain.StayPeriod;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface BookingRepositoryContractTest {

  BookingRepository repository();

  @Test
  default void shouldSaveAndFindBookingById() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 12)),
            2);

    UUID holdId = UUID.randomUUID();
    booking.placeOnHold(holdId);

    Booking savedBooking = repository().save(booking);

    Optional<Booking> foundBooking = repository().findById(savedBooking.getId());

    assertTrue(foundBooking.isPresent());
    assertEquals(savedBooking.getId(), foundBooking.get().getId());
    assertEquals(savedBooking.getHotelId(), foundBooking.get().getHotelId());
    assertEquals(savedBooking.getRoomTypeId(), foundBooking.get().getRoomTypeId());
    assertEquals(savedBooking.getStayPeriod(), foundBooking.get().getStayPeriod());
    assertEquals(savedBooking.getGuestCount(), foundBooking.get().getGuestCount());
    assertEquals(BookingStatus.ON_HOLD, foundBooking.get().getStatus());
    assertEquals(holdId, foundBooking.get().getHoldId());
  }

  @Test
  default void shouldUpdateExistingBooking() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 12)),
            2);

    booking.placeOnHold(UUID.randomUUID());
    repository().save(booking);

    booking.confirmHeldBooking();
    repository().save(booking);

    Optional<Booking> foundBooking = repository().findById(booking.getId());

    assertTrue(foundBooking.isPresent());
    assertEquals(BookingStatus.CONFIRMED, foundBooking.get().getStatus());
    assertNull(foundBooking.get().getHoldId());
  }

  @Test
  default void shouldReturnEmptyWhenBookingIsNotFound() {
    assertTrue(repository().findById(BookingId.newId()).isEmpty());
  }
}
