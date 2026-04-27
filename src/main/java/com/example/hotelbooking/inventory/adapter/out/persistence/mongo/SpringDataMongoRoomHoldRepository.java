package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataMongoRoomHoldRepository extends MongoRepository<MongoRoomHoldDocument, UUID> {}
