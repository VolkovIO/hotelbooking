package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document("room_availability")
class MongoRoomAvailabilityDocument {

  @Id private String id;

  private UUID hotelId;

  private UUID roomTypeId;

  private LocalDate date;

  private int totalRooms;

  private int heldRooms;

  private int bookedRooms;

  MongoRoomAvailabilityDocument(
      UUID hotelId,
      UUID roomTypeId,
      LocalDate date,
      int totalRooms,
      int heldRooms,
      int bookedRooms) {
    this.id = buildId(hotelId, roomTypeId, date);
    this.hotelId = hotelId;
    this.roomTypeId = roomTypeId;
    this.date = date;
    this.totalRooms = totalRooms;
    this.heldRooms = heldRooms;
    this.bookedRooms = bookedRooms;
  }

  static String buildId(UUID hotelId, UUID roomTypeId, LocalDate date) {
    return hotelId + ":" + roomTypeId + ":" + date;
  }
}
