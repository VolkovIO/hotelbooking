package com.example.hotelbooking.inventory.application.port.in;

import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetRoomAvailabilityUseCase {
  List<RoomAvailability> execute(UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to);
}
