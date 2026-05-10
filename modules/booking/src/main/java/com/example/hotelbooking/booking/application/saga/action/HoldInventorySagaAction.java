package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.application.service.BookingStateChangePersistenceService;
import com.example.hotelbooking.booking.domain.Booking;
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

    UUID holdId =
        inventoryReservationPort.placeHold(
            booking.getHotelId(),
            booking.getRoomTypeId(),
            booking.getStayPeriod().checkIn(),
            booking.getStayPeriod().checkOut(),
            ROOMS_PER_BOOKING);

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
}
