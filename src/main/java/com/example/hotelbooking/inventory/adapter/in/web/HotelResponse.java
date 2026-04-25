package com.example.hotelbooking.inventory.adapter.in.web;

import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Hotel response")
public record HotelResponse(
    @Schema(description = "Hotel identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID hotelId,
    @Schema(description = "Hotel name", example = "Riviera Hotel") String name,
    @Schema(description = "Hotel city", example = "Kazan") String city,
    @Schema(description = "Room types registered in the hotel") List<RoomTypeResponse> roomTypes) {

  public HotelResponse {
    roomTypes = roomTypes == null ? List.of() : List.copyOf(roomTypes);
  }

  public static HotelResponse from(Hotel hotel) {
    return new HotelResponse(
        hotel.getId(),
        hotel.getName(),
        hotel.getCity(),
        hotel.getRoomTypes().stream().map(RoomTypeResponse::from).toList());
  }

  public record RoomTypeResponse(
      @Schema(
              description = "Room type identifier",
              example = "660e8400-e29b-41d4-a716-446655440000")
          UUID roomTypeId,
      @Schema(description = "Room type name", example = "Standard") String name,
      @Schema(description = "Guest capacity", example = "2") int guestCapacity) {

    public static RoomTypeResponse from(RoomType roomType) {
      return new RoomTypeResponse(
          roomType.getId(), roomType.getName(), roomType.getGuestCapacity());
    }
  }
}
