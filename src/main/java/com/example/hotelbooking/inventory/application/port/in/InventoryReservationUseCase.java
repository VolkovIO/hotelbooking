package com.example.hotelbooking.inventory.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface InventoryReservationUseCase {

  UUID placeHold(UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms);

  void confirmHold(UUID holdId);

  void releaseHold(UUID holdId);

  void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms);
}
