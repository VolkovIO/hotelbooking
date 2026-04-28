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
@Document("room_holds")
class MongoRoomHoldDocument {

  @Id private UUID id;

  private UUID hotelId;

  private UUID roomTypeId;

  private LocalDate checkIn;

  private LocalDate checkOut;

  private int rooms;
}
