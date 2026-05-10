package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.application.service.BookingStateChangePersistenceService;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import com.example.hotelbooking.booking.domain.BookingStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseInventorySagaAction implements BookingSagaAction {

  private static final int ROOMS_PER_BOOKING = 1;

  private final BookingSagaRepository sagaRepository;
  private final BookingSagaBookingLoader bookingLoader;
  private final InventoryReservationPort inventoryReservationPort;
  private final BookingStateChangePersistenceService bookingStateChangePersistenceService;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.RELEASE_INVENTORY;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    Booking booking = bookingLoader.load(saga);

    if (booking.isOnHold()) {
      releaseHeldBookingInventory(saga, booking);
    } else if (booking.isConfirmed()) {
      cancelConfirmedBookingInventory(saga, booking);
    } else {
      log.debug(
          "Booking does not need inventory release: sagaId={}, bookingId={}, status={}",
          saga.getId().value(),
          booking.getId().value(),
          booking.getStatus());
    }

    saga.markInventoryReleased();
    sagaRepository.save(saga);

    return saga;
  }

  private void releaseHeldBookingInventory(BookingSaga saga, Booking booking) {
    final BookingStatus previousStatus = booking.getStatus();
    UUID holdId = booking.getHoldId();

    if (holdId == null) {
      throw new BookingDomainException("Booking has no active hold to release");
    }

    inventoryReservationPort.releaseHold(holdId);

    booking.cancelHeldBooking();

    bookingStateChangePersistenceService.persist(
        booking, BookingLifecycleEvent.cancelled(booking, previousStatus));

    log.info(
        "Inventory hold released by saga compensation: sagaId={}, bookingId={}, holdId={}",
        saga.getId().value(),
        booking.getId().value(),
        holdId);
  }

  private void cancelConfirmedBookingInventory(BookingSaga saga, Booking booking) {
    final BookingStatus previousStatus = booking.getStatus();

    inventoryReservationPort.cancelConfirmedReservation(
        booking.getHotelId(),
        booking.getRoomTypeId(),
        booking.getStayPeriod().checkIn(),
        booking.getStayPeriod().checkOut(),
        ROOMS_PER_BOOKING);

    booking.cancelConfirmedBooking();

    bookingStateChangePersistenceService.persist(
        booking, BookingLifecycleEvent.cancelled(booking, previousStatus));

    log.info(
        "Confirmed inventory reservation cancelled by saga compensation: sagaId={}, bookingId={}",
        saga.getId().value(),
        booking.getId().value());
  }
}
