package com.example.hotelbooking.inventory.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class RoomHold {

  private final UUID id;
  private final UUID hotelId;
  private final UUID roomTypeId;
  private final LocalDate checkIn;
  private final LocalDate checkOut;
  private final int rooms;

  private RoomHold(
      UUID id, UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.hotelId = Objects.requireNonNull(hotelId, "hotelId must not be null");
    this.roomTypeId = Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    this.checkIn = Objects.requireNonNull(checkIn, "checkIn must not be null");
    this.checkOut = Objects.requireNonNull(checkOut, "checkOut must not be null");
    this.rooms = requirePositive(rooms, "rooms must be positive");

    if (!checkOut.isAfter(checkIn)) {
      throw new InventoryDomainException("checkOut must be after checkIn");
    }
  }

  public static RoomHold create(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    return new RoomHold(UUID.randomUUID(), hotelId, roomTypeId, checkIn, checkOut, rooms);
  }

  public static RoomHold restore(
      UUID id, UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    return new RoomHold(id, hotelId, roomTypeId, checkIn, checkOut, rooms);
  }

  public UUID getId() {
    return id;
  }

  public UUID getHotelId() {
    return hotelId;
  }

  public UUID getRoomTypeId() {
    return roomTypeId;
  }

  public LocalDate getCheckIn() {
    return checkIn;
  }

  public LocalDate getCheckOut() {
    return checkOut;
  }

  public int getRooms() {
    return rooms;
  }

  private static int requirePositive(int value, String message) {
    if (value <= 0) {
      throw new InventoryDomainException(message);
    }
    return value;
  }
}
