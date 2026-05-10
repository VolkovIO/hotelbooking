package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.application.service.BookingStateChangePersistenceService;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldInventorySagaAction implements BookingSagaAction {

  private static final int ROOMS_PER_BOOKING = 1;

  private final BookingSagaRepository sagaRepository;
  private final BookingRepository bookingRepository;
  private final BookingSagaBookingLoader bookingLoader;
  private final InventoryReservationPort inventoryReservationPort;
  private final BookingStateChangePersistenceService bookingStateChangePersistenceService;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.HOLD_INVENTORY;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    Booking booking = bookingLoader.load(saga);

    if (booking.isOnHold()) {
      log.debug(
          "Booking is already on hold, skipping inventory hold: bookingId={}",
          booking.getId().value());

      saga.markInventoryHeld();
      return sagaRepository.save(saga);
    }

    UUID holdId = placeInventoryHold(saga, booking);

    booking.placeOnHold(holdId);

    bookingStateChangePersistenceService.persist(
        booking, BookingLifecycleEvent.placedOnHold(booking));

    saga.markInventoryHeld();
    sagaRepository.save(saga);

    log.info(
        "Inventory hold placed by saga: sagaId={}, bookingId={}, holdId={}",
        saga.getId().value(),
        booking.getId().value(),
        holdId);

    return saga;
  }

  private UUID placeInventoryHold(BookingSaga saga, Booking booking) {
    try {
      return inventoryReservationPort.placeHold(
          booking.getHotelId(),
          booking.getRoomTypeId(),
          booking.getStayPeriod().checkIn(),
          booking.getStayPeriod().checkOut(),
          ROOMS_PER_BOOKING);
    } catch (RoomHoldFailedException exception) {
      rejectBookingBeforeHold(saga, booking, exception);
      throw exception;
    }
  }

  private void rejectBookingBeforeHold(
      BookingSaga saga, Booking booking, RoomHoldFailedException exception) {
    if (booking.getStatus() != BookingStatus.NEW) {
      return;
    }

    booking.reject();
    bookingRepository.save(booking);

    log.info(
        "Booking rejected because inventory hold could not be placed: sagaId={}, bookingId={}, reason={}",
        saga.getId().value(),
        booking.getId().value(),
        failureMessage(exception));
  }

  private String failureMessage(RuntimeException exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }

    Throwable cause = exception.getCause();
    if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) {
      return message;
    }

    return message + ": " + cause.getMessage();
  }
}
