package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import com.example.hotelbooking.inventory.application.port.out.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
class MongoRoomHoldRepositoryAdapter implements RoomHoldRepository {

  private final SpringDataMongoRoomHoldRepository repository;

  @Override
  public RoomHold save(RoomHold roomHold) {
    MongoRoomHoldDocument savedDocument =
        repository.save(MongoInventoryMapper.toDocument(roomHold));
    return MongoInventoryMapper.toDomain(savedDocument);
  }

  @Override
  public Optional<RoomHold> findById(UUID holdId) {
    return repository.findById(holdId).map(MongoInventoryMapper::toDomain);
  }

  @Override
  public void deleteById(UUID holdId) {
    repository.deleteById(holdId);
  }
}
