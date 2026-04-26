package com.example.hotelbooking.booking.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

public interface InventoryReservationPort {

  UUID placeHold(UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms);

  void releaseHold(UUID holdId);

  void confirmHold(UUID holdId);

  void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms);
}
