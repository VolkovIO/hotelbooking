package com.example.hotelbooking.inventory.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class RoomAvailability {

  private final UUID hotelId;
  private final UUID roomTypeId;
  private final LocalDate date;
  private final int totalRooms;

  private RoomAvailability(UUID hotelId, UUID roomTypeId, LocalDate date, int totalRooms) {
    this.hotelId = Objects.requireNonNull(hotelId, "hotelId must not be null");
    this.roomTypeId = Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    this.date = Objects.requireNonNull(date, "date must not be null");
    this.totalRooms = requireNonNegative(totalRooms, "totalRooms must not be negative");
  }

  public static RoomAvailability create(
      UUID hotelId, UUID roomTypeId, LocalDate date, int totalRooms) {
    return new RoomAvailability(hotelId, roomTypeId, date, totalRooms);
  }

  public UUID getHotelId() {
    return hotelId;
  }

  public UUID getRoomTypeId() {
    return roomTypeId;
  }

  public LocalDate getDate() {
    return date;
  }

  public int getTotalRooms() {
    return totalRooms;
  }

  private static int requireNonNegative(int value, String message) {
    if (value < 0) {
      throw new InventoryDomainException(message);
    }
    return value;
  }
}
