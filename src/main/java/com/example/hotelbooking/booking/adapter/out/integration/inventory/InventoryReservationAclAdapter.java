package com.example.hotelbooking.booking.adapter.out.integration.inventory;

import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.inventory.application.exception.RoomHoldAvailabilityIncompleteException;
import com.example.hotelbooking.inventory.application.exception.RoomHoldNotFoundException;
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
    try {
      inventoryReservationUseCase.releaseHold(holdId);
    } catch (InventoryDomainException
        | RoomHoldNotFoundException
        | RoomHoldAvailabilityIncompleteException exception) {
      throw roomHoldFailed("release", holdId, exception);
    }
  }

  @Override
  public void confirmHold(UUID holdId) {
    try {
      inventoryReservationUseCase.confirmHold(holdId);
    } catch (InventoryDomainException
        | RoomHoldNotFoundException
        | RoomHoldAvailabilityIncompleteException exception) {
      throw roomHoldFailed("confirm", holdId, exception);
    }
  }

  private static RoomHoldFailedException roomHoldFailed(
      String operation, UUID holdId, RuntimeException cause) {
    return new RoomHoldFailedException("Failed to " + operation + " room hold: " + holdId, cause);
  }

  @Override
  public void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    try {
      inventoryReservationUseCase.cancelConfirmedReservation(
          hotelId, roomTypeId, checkIn, checkOut, rooms);
    } catch (InventoryDomainException exception) {
      throw new RoomHoldFailedException(
          "Failed to cancel confirmed reservation for hotel %s and room type %s"
              .formatted(hotelId, roomTypeId),
          exception);
    }
  }
}
