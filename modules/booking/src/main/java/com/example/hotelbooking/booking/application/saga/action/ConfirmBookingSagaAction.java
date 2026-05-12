package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.application.service.BookingStateChangePersistenceService;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmBookingSagaAction implements BookingSagaAction {

  private final BookingSagaRepository sagaRepository;
  private final BookingSagaBookingLoader bookingLoader;
  private final InventoryReservationPort inventoryReservationPort;
  private final BookingStateChangePersistenceService bookingStateChangePersistenceService;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.CONFIRM_BOOKING;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    Booking booking = bookingLoader.load(saga);

    if (booking.isConfirmed()) {
      log.debug(
          "Booking is already confirmed, skipping booking confirmation: bookingId={}",
          booking.getId().value());

      saga.markBookingConfirmed();
      return sagaRepository.save(saga);
    }

    UUID confirmedHoldId = booking.getHoldId();
    if (confirmedHoldId == null) {
      throw new BookingDomainException("Booking has no active hold to confirm");
    }

    inventoryReservationPort.confirmHold(confirmedHoldId);

    booking.confirmHeldBooking();

    UUID correlationId = saga.getId().value();

    bookingStateChangePersistenceService.persist(
        booking, BookingLifecycleEvent.confirmed(booking, confirmedHoldId, correlationId, null));

    saga.markBookingConfirmed();
    sagaRepository.save(saga);

    log.info(
        "Booking confirmed by saga: sagaId={}, bookingId={}",
        saga.getId().value(),
        booking.getId().value());

    return saga;
  }
}
