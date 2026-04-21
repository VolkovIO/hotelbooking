package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.port.BookingRepository;
import com.example.hotelbooking.booking.application.port.InventoryReservationPort;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelBookingUseCase {

  private final BookingRepository bookingRepository;
  private final InventoryReservationPort inventoryReservationPort;

  public Booking execute(CancelBookingCommand command) {
    Booking booking =
        bookingRepository
            .findById(command.bookingId())
            .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

    UUID holdId = booking.getHoldId();
    if (holdId == null) {
      throw new BookingDomainException("Booking has no active hold to cancel");
    }

    inventoryReservationPort.releaseHold(holdId);
    booking.cancelHeldBooking();

    return bookingRepository.save(booking);
  }
}
