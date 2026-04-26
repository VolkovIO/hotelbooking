package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.command.CancelBookingCommand;
import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.port.in.CancelBookingUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelBookingService implements CancelBookingUseCase {

  private static final int ROOMS_PER_BOOKING = 1;

  private final BookingRepository bookingRepository;
  private final InventoryReservationPort inventoryReservationPort;

  @Override
  public Booking execute(CancelBookingCommand command) {
    Booking booking =
        bookingRepository
            .findById(command.bookingId())
            .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

    if (booking.isOnHold()) {
      cancelHeldBooking(booking);
    } else if (booking.isConfirmed()) {
      cancelConfirmedBooking(booking);
    } else {
      throw new BookingDomainException("Only ON_HOLD or CONFIRMED booking can be cancelled");
    }

    return bookingRepository.save(booking);
  }

  private void cancelHeldBooking(Booking booking) {
    UUID holdId = booking.getHoldId();
    if (holdId == null) {
      throw new BookingDomainException("Booking has no active hold to cancel");
    }

    inventoryReservationPort.releaseHold(holdId);
    booking.cancelHeldBooking();
  }

  private void cancelConfirmedBooking(Booking booking) {
    inventoryReservationPort.cancelConfirmedReservation(
        booking.getHotelId(),
        booking.getRoomTypeId(),
        booking.getStayPeriod().checkIn(),
        booking.getStayPeriod().checkOut(),
        ROOMS_PER_BOOKING);

    booking.cancelConfirmedBooking();
  }
}
