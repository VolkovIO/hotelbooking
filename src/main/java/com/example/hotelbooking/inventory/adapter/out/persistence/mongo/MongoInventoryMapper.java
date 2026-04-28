package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomHold;
import com.example.hotelbooking.inventory.domain.RoomType;
import java.util.List;

final class MongoInventoryMapper {

  private MongoInventoryMapper() {}

  static MongoHotelDocument toDocument(Hotel hotel) {
    List<MongoRoomTypeDocument> roomTypes =
        hotel.getRoomTypes().stream()
            .map(
                roomType ->
                    new MongoRoomTypeDocument(
                        roomType.getId(), roomType.getName(), roomType.getGuestCapacity()))
            .toList();

    return new MongoHotelDocument(hotel.getId(), hotel.getName(), hotel.getCity(), roomTypes);
  }

  static MongoRoomAvailabilityDocument toDocument(RoomAvailability availability) {
    return new MongoRoomAvailabilityDocument(
        availability.getHotelId(),
        availability.getRoomTypeId(),
        availability.getDate(),
        availability.getTotalRooms(),
        availability.getHeldRooms(),
        availability.getBookedRooms());
  }

  static MongoRoomHoldDocument toDocument(RoomHold hold) {
    return new MongoRoomHoldDocument(
        hold.getId(),
        hold.getHotelId(),
        hold.getRoomTypeId(),
        hold.getCheckIn(),
        hold.getCheckOut(),
        hold.getRooms());
  }

  static Hotel toDomain(MongoHotelDocument document) {
    List<RoomType> roomTypes =
        document.getRoomTypes().stream()
            .map(
                roomType ->
                    RoomType.restore(
                        roomType.getRoomTypeId(), roomType.getName(), roomType.getGuestCapacity()))
            .toList();

    return Hotel.restore(document.getId(), document.getName(), document.getCity(), roomTypes);
  }

  static RoomAvailability toDomain(MongoRoomAvailabilityDocument document) {
    return RoomAvailability.restore(
        document.getHotelId(),
        document.getRoomTypeId(),
        document.getDate(),
        document.getTotalRooms(),
        document.getHeldRooms(),
        document.getBookedRooms());
  }

  static RoomHold toDomain(MongoRoomHoldDocument document) {
    return RoomHold.restore(
        document.getId(),
        document.getHotelId(),
        document.getRoomTypeId(),
        document.getCheckIn(),
        document.getCheckOut(),
        document.getRooms());
  }
}
