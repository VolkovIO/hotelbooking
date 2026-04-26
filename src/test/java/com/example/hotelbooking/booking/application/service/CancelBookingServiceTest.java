package com.example.hotelbooking.booking.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.command.CancelBookingCommand;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.booking.domain.StayPeriod;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelBookingServiceTest {

  @Mock private BookingRepository bookingRepository;

  @Mock private InventoryReservationPort inventoryReservationPort;

  private CancelBookingService service;

  @BeforeEach
  void setUp() {
    service = new CancelBookingService(bookingRepository, inventoryReservationPort);
  }

  @Test
  void shouldCancelConfirmedBookingAndReleaseBookedRooms() {
    Booking booking = confirmedBooking();

    when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
    when(bookingRepository.save(booking)).thenReturn(booking);

    Booking result = service.execute(new CancelBookingCommand(booking.getId()));

    assertEquals(BookingStatus.CANCELLED, result.getStatus());

    verify(inventoryReservationPort)
        .cancelConfirmedReservation(
            booking.getHotelId(),
            booking.getRoomTypeId(),
            booking.getStayPeriod().checkIn(),
            booking.getStayPeriod().checkOut(),
            1);
    verify(bookingRepository).save(booking);
  }

  private Booking confirmedBooking() {
    Booking booking =
        Booking.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20)),
            2);

    booking.placeOnHold(UUID.randomUUID());
    booking.confirmHeldBooking();

    return booking;
  }
}
