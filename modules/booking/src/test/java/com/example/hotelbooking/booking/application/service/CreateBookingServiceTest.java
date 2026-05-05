package com.example.hotelbooking.booking.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.exception.GuestCountExceedsRoomCapacityException;
import com.example.hotelbooking.booking.application.exception.RoomTypeReferenceNotFoundException;
import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.port.out.RoomTypeReference;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import com.example.hotelbooking.booking.domain.UserId;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateBookingServiceTest {

  @Mock private BookingStateChangePersistenceService bookingStateChangePersistenceService;

  @Mock private InventoryLookupPort inventoryLookupPort;

  @Mock private InventoryReservationPort inventoryReservationPort;

  @InjectMocks private CreateBookingService service;

  @Test
  void shouldValidateStayPeriodBeforeCallingInventory() {
    CreateBookingCommand command =
        new CreateBookingCommand(
            userId(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDate.of(2030, 6, 20),
            LocalDate.of(2030, 6, 10),
            2);

    assertThrows(BookingDomainException.class, () -> service.execute(command));
    verifyNoInteractions(
        inventoryLookupPort, inventoryReservationPort, bookingStateChangePersistenceService);
  }

  @Test
  void shouldNotPlaceInventoryHoldWhenGuestCountExceedsRoomCapacity() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();

    when(inventoryLookupPort.findRoomTypeReference(hotelId, roomTypeId))
        .thenReturn(Optional.of(new RoomTypeReference(hotelId, roomTypeId, 2)));

    CreateBookingCommand command =
        new CreateBookingCommand(
            userId(), hotelId, roomTypeId, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20), 3);

    assertThrows(GuestCountExceedsRoomCapacityException.class, () -> service.execute(command));

    verifyNoInteractions(inventoryReservationPort, bookingStateChangePersistenceService);
  }

  @Test
  void shouldNotPlaceInventoryHoldWhenRoomTypeReferenceIsNotFound() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();

    when(inventoryLookupPort.findRoomTypeReference(hotelId, roomTypeId))
        .thenReturn(Optional.empty());

    CreateBookingCommand command =
        new CreateBookingCommand(
            userId(), hotelId, roomTypeId, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20), 2);

    assertThrows(RoomTypeReferenceNotFoundException.class, () -> service.execute(command));

    verifyNoInteractions(inventoryReservationPort, bookingStateChangePersistenceService);
  }

  private UserId userId() {
    return new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  }
}
