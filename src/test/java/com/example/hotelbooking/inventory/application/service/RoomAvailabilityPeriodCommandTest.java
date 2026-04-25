package com.example.hotelbooking.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomAvailabilityPeriodCommandTest {

  @Test
  void shouldCreateCommandWhenArgumentsAreValid() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate startDate = LocalDate.of(2030, 6, 10);
    LocalDate endDate = LocalDate.of(2030, 6, 12);

    assertDoesNotThrow(
        () -> new RoomAvailabilityPeriodCommand(hotelId, roomTypeId, startDate, endDate, 5));
  }

  @Test
  void shouldRejectCommandWhenEndDateIsBeforeStartDate() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate startDate = LocalDate.of(2030, 6, 12);
    LocalDate endDate = LocalDate.of(2030, 6, 10);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RoomAvailabilityPeriodCommand(hotelId, roomTypeId, startDate, endDate, 5));

    assertEquals("endDate must not be before startDate", exception.getMessage());
  }

  @Test
  void shouldRejectCommandWhenTotalRoomsIsNotPositive() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate startDate = LocalDate.of(2030, 6, 10);
    LocalDate endDate = LocalDate.of(2030, 6, 12);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RoomAvailabilityPeriodCommand(hotelId, roomTypeId, startDate, endDate, 0));

    assertEquals("totalRooms must be positive", exception.getMessage());
  }
}
