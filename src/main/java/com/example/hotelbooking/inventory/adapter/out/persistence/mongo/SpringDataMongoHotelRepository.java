package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataMongoHotelRepository extends MongoRepository<MongoHotelDocument, UUID> {
  List<MongoHotelDocument> findAllBy(Pageable pageable);
}
