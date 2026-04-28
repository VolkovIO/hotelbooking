package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
class MongoRoomTypeDocument {

  private UUID roomTypeId;

  private String name;

  private int guestCapacity;
}
