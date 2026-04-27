package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document("hotels")
class MongoHotelDocument {

  @Id private UUID id;

  private String name;

  private String city;

  private List<MongoRoomTypeDocument> roomTypes;
}
