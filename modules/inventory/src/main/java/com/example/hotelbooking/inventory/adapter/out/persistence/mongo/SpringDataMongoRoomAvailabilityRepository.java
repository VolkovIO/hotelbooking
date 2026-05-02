package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

interface SpringDataMongoRoomAvailabilityRepository
    extends MongoRepository<MongoRoomAvailabilityDocument, String> {

  Optional<MongoRoomAvailabilityDocument> findByHotelIdAndRoomTypeIdAndDate(
      UUID hotelId, UUID roomTypeId, LocalDate date);

  @Query(
      value = "{ 'hotelId': ?0, 'roomTypeId': ?1, 'date': { '$gte': ?2, '$lte': ?3 } }",
      sort = "{ 'date': 1 }")
  List<MongoRoomAvailabilityDocument> findByRoomTypeAndDateRange(
      UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to);
}
