package com.example.hotelbooking.inventory.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class RoomAvailability {

  private final UUID hotelId;
  private final UUID roomTypeId;
  private final LocalDate date;
  private final int totalRooms;
  private final int heldRooms;

  private RoomAvailability(
      UUID hotelId, UUID roomTypeId, LocalDate date, int totalRooms, int heldRooms) {
    this.hotelId = Objects.requireNonNull(hotelId, "hotelId must not be null");
    this.roomTypeId = Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    this.date = Objects.requireNonNull(date, "date must not be null");
    this.totalRooms = requireNonNegative(totalRooms, "totalRooms must not be negative");
    this.heldRooms = requireNonNegative(heldRooms, "heldRooms must not be negative");

    if (heldRooms > totalRooms) {
      throw new InventoryDomainException("heldRooms must not exceed totalRooms");
    }
  }

  public static RoomAvailability create(
      UUID hotelId, UUID roomTypeId, LocalDate date, int totalRooms) {
    return new RoomAvailability(hotelId, roomTypeId, date, totalRooms, 0);
  }

  public RoomAvailability placeHold(int rooms) {
    int normalizedRooms = requirePositive(rooms, "rooms must be positive");

    if (availableRooms() < normalizedRooms) {
      throw new InventoryDomainException("Not enough rooms available to place hold");
    }

    return new RoomAvailability(hotelId, roomTypeId, date, totalRooms, heldRooms + normalizedRooms);
  }

  public RoomAvailability releaseHold(int rooms) {
    int normalizedRooms = requirePositive(rooms, "rooms must be positive");

    if (heldRooms < normalizedRooms) {
      throw new InventoryDomainException("Cannot release more held rooms than currently held");
    }

    return new RoomAvailability(hotelId, roomTypeId, date, totalRooms, heldRooms - normalizedRooms);
  }

  public int availableRooms() {
    return totalRooms - heldRooms;
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

  public int getHeldRooms() {
    return heldRooms;
  }

  private static int requireNonNegative(int value, String message) {
    if (value < 0) {
      throw new InventoryDomainException(message);
    }
    return value;
  }

  private static int requirePositive(int value, String message) {
    if (value <= 0) {
      throw new InventoryDomainException(message);
    }
    return value;
  }
}
