package com.example.hotelbooking.inventory.adapter.in.web;

import com.example.hotelbooking.inventory.application.port.in.HotelSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Hotel summary with available room types")
public record HotelSummaryResponse(
    @Schema(description = "Hotel id") UUID hotelId,
    @Schema(description = "Hotel name", example = "Demo Kazan Hotel") String name,
    @Schema(description = "Hotel city", example = "Kazan") String city,
    @Schema(description = "Room types") List<RoomTypeSummaryResponse> roomTypes) {

  public HotelSummaryResponse {
    roomTypes = List.copyOf(roomTypes);
  }

  static HotelSummaryResponse from(HotelSummaryResult result) {
    return new HotelSummaryResponse(
        result.hotelId(),
        result.name(),
        result.city(),
        result.roomTypes().stream().map(RoomTypeSummaryResponse::from).toList());
  }
}
