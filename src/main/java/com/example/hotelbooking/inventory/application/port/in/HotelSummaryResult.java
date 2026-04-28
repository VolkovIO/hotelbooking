package com.example.hotelbooking.inventory.application.port.in;

import java.util.List;
import java.util.UUID;

public record HotelSummaryResult(
    UUID hotelId, String name, String city, List<RoomTypeSummaryResult> roomTypes) {

  public HotelSummaryResult {
    roomTypes = List.copyOf(roomTypes);
  }
}
