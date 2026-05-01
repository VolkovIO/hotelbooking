package com.example.hotelbooking.inventory.adapter.in.web;

import com.example.hotelbooking.inventory.domain.RoomAvailability;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Daily room availability")
public record RoomAvailabilityResponse(
    @Schema(description = "Availability date", example = "2030-06-10") LocalDate date,
    @Schema(description = "Configured total rooms", example = "10") int totalRooms,
    @Schema(description = "Currently held rooms", example = "1") int heldRooms,
    @Schema(description = "Currently booked rooms", example = "2") int bookedRooms,
    @Schema(description = "Currently available rooms", example = "7") int availableRooms) {

  public static RoomAvailabilityResponse from(RoomAvailability roomAvailability) {
    return new RoomAvailabilityResponse(
        roomAvailability.getDate(),
        roomAvailability.getTotalRooms(),
        roomAvailability.getHeldRooms(),
        roomAvailability.getBookedRooms(),
        roomAvailability.availableRooms());
  }
}
