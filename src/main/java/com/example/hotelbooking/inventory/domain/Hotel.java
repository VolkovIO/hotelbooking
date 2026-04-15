package com.example.hotelbooking.inventory.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Hotel {

  private final UUID id;
  private final String name;
  private final String city;
  private final List<RoomType> roomTypes;

  private Hotel(UUID id, String name, String city, List<RoomType> roomTypes) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.name = requireNotBlank(name, "name must not be blank");
    this.city = requireNotBlank(city, "city must not be blank");
    this.roomTypes =
        new ArrayList<>(Objects.requireNonNull(roomTypes, "roomTypes must not be null"));
  }

  public static Hotel create(String name, String city) {
    return new Hotel(UUID.randomUUID(), name, city, List.of());
  }

  public void addRoomType(String name, int guestCapacity) {
    boolean roomTypeAlreadyExists =
        roomTypes.stream().anyMatch(roomType -> roomType.getName().equalsIgnoreCase(name));

    if (roomTypeAlreadyExists) {
      throw new InventoryDomainException("Room type with this name already exists in the hotel");
    }

    roomTypes.add(RoomType.create(name, guestCapacity));
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  public List<RoomType> getRoomTypes() {
    return List.copyOf(roomTypes);
  }

  private static String requireNotBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new InventoryDomainException(message);
    }
    return value;
  }
}
