package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.inventory.application.exception.RoomHoldNotFoundException;
import com.example.hotelbooking.inventory.application.port.in.InventoryReservationUseCase;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryReservationAclAdapterTest {

  @Mock private InventoryReservationUseCase inventoryReservationUseCase;

  private InventoryReservationAclAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InventoryReservationAclAdapter(inventoryReservationUseCase);
  }

  @Test
  void shouldTranslateInventoryExceptionWhenConfirmingHoldFails() {
    UUID holdId = UUID.randomUUID();
    RoomHoldNotFoundException cause = new RoomHoldNotFoundException(holdId);

    doThrow(cause).when(inventoryReservationUseCase).confirmHold(holdId);

    RoomHoldFailedException exception =
        assertThrows(RoomHoldFailedException.class, () -> adapter.confirmHold(holdId));

    assertSame(cause, exception.getCause());
  }

  @Test
  void shouldTranslateInventoryExceptionWhenReleasingHoldFails() {
    UUID holdId = UUID.randomUUID();
    RoomHoldNotFoundException cause = new RoomHoldNotFoundException(holdId);

    doThrow(cause).when(inventoryReservationUseCase).releaseHold(holdId);

    RoomHoldFailedException exception =
        assertThrows(RoomHoldFailedException.class, () -> adapter.releaseHold(holdId));

    assertSame(cause, exception.getCause());
  }

  @Test
  void shouldTranslateInventoryExceptionWhenPlacingHoldFails() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate checkIn = LocalDate.of(2030, 6, 10);
    LocalDate checkOut = LocalDate.of(2030, 6, 20);
    int rooms = 1;

    InventoryDomainException cause = new InventoryDomainException("test inventory failure");

    when(inventoryReservationUseCase.placeHold(hotelId, roomTypeId, checkIn, checkOut, rooms))
        .thenThrow(cause);

    RoomHoldFailedException exception =
        assertThrows(
            RoomHoldFailedException.class,
            () -> adapter.placeHold(hotelId, roomTypeId, checkIn, checkOut, rooms));

    assertSame(cause, exception.getCause());
  }

  @Test
  void shouldTranslateInventoryExceptionWhenCancellingConfirmedReservationFails() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate checkIn = LocalDate.of(2030, 6, 10);
    LocalDate checkOut = LocalDate.of(2030, 6, 20);
    int rooms = 1;

    InventoryDomainException cause = new InventoryDomainException("test inventory failure");

    doThrow(cause)
        .when(inventoryReservationUseCase)
        .cancelConfirmedReservation(hotelId, roomTypeId, checkIn, checkOut, rooms);

    RoomHoldFailedException exception =
        assertThrows(
            RoomHoldFailedException.class,
            () ->
                adapter.cancelConfirmedReservation(hotelId, roomTypeId, checkIn, checkOut, rooms));

    assertSame(cause, exception.getCause());
  }
}
