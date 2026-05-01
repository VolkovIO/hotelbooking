package com.example.hotelbooking.inventory.domain;

import java.util.Objects;
import java.util.UUID;

public final class RoomType {

  private final UUID id;
  private final String name;
  private final int guestCapacity;

  private RoomType(UUID id, String name, int guestCapacity) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.name = requireNotBlank(name, "name must not be blank");
    this.guestCapacity = requirePositive(guestCapacity, "guestCapacity must be positive");
  }

  public static RoomType create(String name, int guestCapacity) {
    return new RoomType(UUID.randomUUID(), name, guestCapacity);
  }

  public static RoomType restore(UUID id, String name, int guestCapacity) {
    return new RoomType(id, name, guestCapacity);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getGuestCapacity() {
    return guestCapacity;
  }

  private static String requireNotBlank(String value, String message) {
    if (value == null || value.isBlank()) {
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
