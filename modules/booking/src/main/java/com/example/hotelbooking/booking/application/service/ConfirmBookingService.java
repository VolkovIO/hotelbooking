package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.command.ConfirmBookingCommand;
import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.exception.BookingAccessDeniedException;
import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.port.in.ConfirmBookingUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmBookingService implements ConfirmBookingUseCase {

  private final BookingRepository bookingRepository;
  private final InventoryReservationPort inventoryReservationPort;
  private final BookingStateChangePersistenceService bookingStateChangePersistenceService;

  @Override
  public Booking execute(ConfirmBookingCommand command) {
    log.info("Confirming booking: bookingId={}", command.bookingId());

    Booking booking =
        bookingRepository
            .findById(command.bookingId())
            .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

    if (!booking.isOwnedBy(command.userId())) {
      throw new BookingAccessDeniedException(command.bookingId());
    }

    UUID holdId = booking.getHoldId();
    if (holdId == null) {
      throw new BookingDomainException("Booking has no active hold to confirm");
    }

    inventoryReservationPort.confirmHold(holdId);
    log.debug(
        "Inventory hold confirmed for booking: bookingId={}, holdId={}", booking.getId(), holdId);

    booking.confirmHeldBooking();

    Booking savedBooking =
        bookingStateChangePersistenceService.persist(
            booking, BookingLifecycleEvent.confirmed(booking, holdId));

    log.info(
        "Booking confirmed: bookingId={}, status={}",
        savedBooking.getId(),
        savedBooking.getStatus());

    return savedBooking;
  }
}
