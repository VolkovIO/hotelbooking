package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
class MongoHotelRepositoryAdapter implements HotelRepository {

  private final SpringDataMongoHotelRepository repository;

  @Override
  public Hotel save(Hotel hotel) {
    MongoHotelDocument savedDocument = repository.save(MongoInventoryMapper.toDocument(hotel));
    return MongoInventoryMapper.toDomain(savedDocument);
  }

  @Override
  public Optional<Hotel> findById(UUID hotelId) {
    return repository.findById(hotelId).map(MongoInventoryMapper::toDomain);
  }
}
