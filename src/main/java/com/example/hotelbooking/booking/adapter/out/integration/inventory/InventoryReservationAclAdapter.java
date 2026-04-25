package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.inventory.application.port.in.InventoryReservationUseCase;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class InventoryReservationAclAdapter implements InventoryReservationPort {

  private final InventoryReservationUseCase inventoryReservationUseCase;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    try {
      return inventoryReservationUseCase.placeHold(hotelId, roomTypeId, checkIn, checkOut, rooms);
    } catch (InventoryDomainException exception) {
      throw new RoomHoldFailedException(
          "Failed to place room hold for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }

  @Override
  public void releaseHold(UUID holdId) {
    inventoryReservationUseCase.releaseHold(holdId);
  }

  @Override
  public void confirmHold(UUID holdId) {
    inventoryReservationUseCase.confirmHold(holdId);
  }
}
