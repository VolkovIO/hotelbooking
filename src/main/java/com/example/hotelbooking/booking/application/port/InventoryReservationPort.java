package com.example.hotelbooking.booking.application.port;

import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface InventoryReservationPort {

  UUID placeHold(UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms);
}
