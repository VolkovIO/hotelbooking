package com.example.hotelbooking.inventory.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("PMD.CyclomaticComplexity")
public final class RoomAvailability {

  private static final String ROOMS_MUST_BE_POSITIVE = "rooms must be positive";

  private final UUID hotelId;
  private final UUID roomTypeId;
  private final LocalDate date;
  private int totalRooms;
  private final int heldRooms;
  private final int bookedRooms;

  private RoomAvailability(
      UUID hotelId,
      UUID roomTypeId,
      LocalDate date,
      int totalRooms,
      int heldRooms,
      int bookedRooms) {
    this.hotelId = Objects.requireNonNull(hotelId, "hotelId must not be null");
    this.roomTypeId = Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    this.date = Objects.requireNonNull(date, "date must not be null");
    this.totalRooms = requireNonNegative(totalRooms, "totalRooms must not be negative");
    this.heldRooms = requireNonNegative(heldRooms, "heldRooms must not be negative");
    this.bookedRooms = requireNonNegative(bookedRooms, "bookedRooms must not be negative");

    if (heldRooms + bookedRooms > totalRooms) {
      throw new InventoryDomainException("heldRooms plus bookedRooms must not exceed totalRooms");
    }
  }

  public static RoomAvailability create(
      UUID hotelId, UUID roomTypeId, LocalDate date, int totalRooms) {
    return new RoomAvailability(hotelId, roomTypeId, date, totalRooms, 0, 0);
  }

  public RoomAvailability placeHold(int rooms) {
    final int normalizedRooms = requirePositive(rooms, ROOMS_MUST_BE_POSITIVE);

    if (availableRooms() < normalizedRooms) {
      throw new InventoryDomainException("Not enough rooms available to place hold");
    }

    return new RoomAvailability(
        hotelId, roomTypeId, date, totalRooms, heldRooms + normalizedRooms, bookedRooms);
  }

  public RoomAvailability releaseHold(int rooms) {
    final int normalizedRooms = requirePositive(rooms, ROOMS_MUST_BE_POSITIVE);

    if (heldRooms < normalizedRooms) {
      throw new InventoryDomainException("Cannot release more held rooms than currently held");
    }

    return new RoomAvailability(
        hotelId, roomTypeId, date, totalRooms, heldRooms - normalizedRooms, bookedRooms);
  }

  public RoomAvailability releaseBookedRooms(int rooms) {
    final int normalizedRooms = requirePositive(rooms, ROOMS_MUST_BE_POSITIVE);

    if (bookedRooms < normalizedRooms) {
      throw new InventoryDomainException("Cannot release more booked rooms than currently booked");
    }

    return new RoomAvailability(
        hotelId, roomTypeId, date, totalRooms, heldRooms, bookedRooms - normalizedRooms);
  }

  public RoomAvailability confirmHold(int rooms) {
    final int normalizedRooms = requirePositive(rooms, ROOMS_MUST_BE_POSITIVE);

    if (heldRooms < normalizedRooms) {
      throw new InventoryDomainException("Cannot confirm more held rooms than currently held");
    }

    return new RoomAvailability(
        hotelId,
        roomTypeId,
        date,
        totalRooms,
        heldRooms - normalizedRooms,
        bookedRooms + normalizedRooms);
  }

  public void adjustCapacity(int newTotalRooms) {
    if (newTotalRooms <= 0) {
      throw new InventoryDomainException("totalRooms must be positive");
    }

    if (newTotalRooms < heldRooms + bookedRooms) {
      throw new InventoryDomainException("totalRooms cannot be lower than heldRooms + bookedRooms");
    }

    this.totalRooms = newTotalRooms;
  }

  public int availableRooms() {
    return totalRooms - heldRooms - bookedRooms;
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

  public int getBookedRooms() {
    return bookedRooms;
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
