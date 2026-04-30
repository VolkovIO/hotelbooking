package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.inventory.application.exception.InventoryApplicationException;
import com.example.hotelbooking.inventory.application.port.in.InventoryReservationUseCase;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("inventory-direct-client")
@RequiredArgsConstructor
final class InventoryReservationAclAdapter implements InventoryReservationPort {

  private final InventoryReservationUseCase inventoryReservationUseCase;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    try {
      return inventoryReservationUseCase.placeHold(hotelId, roomTypeId, checkIn, checkOut, rooms);
    } catch (InventoryApplicationException | InventoryDomainException exception) {
      throw roomHoldFailed(
          "place room hold for hotel %s and room type %s".formatted(hotelId, roomTypeId),
          exception);
    }
  }

  @Override
  public void releaseHold(UUID holdId) {
    try {
      inventoryReservationUseCase.releaseHold(holdId);
    } catch (InventoryApplicationException | InventoryDomainException exception) {
      throw roomHoldFailed("release room hold: " + holdId, exception);
    }
  }

  @Override
  public void confirmHold(UUID holdId) {
    try {
      inventoryReservationUseCase.confirmHold(holdId);
    } catch (InventoryApplicationException | InventoryDomainException exception) {
      throw roomHoldFailed("confirm room hold: " + holdId, exception);
    }
  }

  @Override
  public void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    try {
      inventoryReservationUseCase.cancelConfirmedReservation(
          hotelId, roomTypeId, checkIn, checkOut, rooms);
    } catch (InventoryApplicationException | InventoryDomainException exception) {
      throw roomHoldFailed(
          "cancel confirmed reservation for hotel %s and room type %s"
              .formatted(hotelId, roomTypeId),
          exception);
    }
  }

  private static RoomHoldFailedException roomHoldFailed(String operation, RuntimeException cause) {
    return new RoomHoldFailedException("Failed to " + operation, cause);
  }
}
