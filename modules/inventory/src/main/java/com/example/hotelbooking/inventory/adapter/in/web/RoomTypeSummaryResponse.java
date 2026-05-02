package com.example.hotelbooking.inventory.adapter.in.web;

import com.example.hotelbooking.inventory.application.port.in.RoomTypeSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Room type summary")
public record RoomTypeSummaryResponse(
    @Schema(description = "Room type id") UUID roomTypeId,
    @Schema(description = "Room type name", example = "STANDARD") String name,
    @Schema(description = "Guest capacity", example = "2") int guestCapacity) {

  static RoomTypeSummaryResponse from(RoomTypeSummaryResult result) {
    return new RoomTypeSummaryResponse(result.roomTypeId(), result.name(), result.guestCapacity());
  }
}
