package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
class MongoRoomAvailabilityRepositoryAdapter implements RoomAvailabilityRepository {

  private final SpringDataMongoRoomAvailabilityRepository repository;

  @Override
  public RoomAvailability save(RoomAvailability roomAvailability) {
    MongoRoomAvailabilityDocument savedDocument =
        repository.save(MongoInventoryMapper.toDocument(roomAvailability));
    return MongoInventoryMapper.toDomain(savedDocument);
  }

  @Override
  public void saveAll(List<RoomAvailability> roomAvailabilityList) {
    List<MongoRoomAvailabilityDocument> documents =
        roomAvailabilityList.stream().map(MongoInventoryMapper::toDocument).toList();

    repository.saveAll(documents);
  }

  @Override
  public Optional<RoomAvailability> findByHotelIdAndRoomTypeIdAndDate(
      UUID hotelId, UUID roomTypeId, LocalDate date) {
    return repository
        .findByHotelIdAndRoomTypeIdAndDate(hotelId, roomTypeId, date)
        .map(MongoInventoryMapper::toDomain);
  }

  @Override
  public List<RoomAvailability> findByRoomTypeAndDateRange(
      UUID hotelId, UUID roomTypeId, LocalDate from, LocalDate to) {
    return repository.findByRoomTypeAndDateRange(hotelId, roomTypeId, from, to).stream()
        .map(MongoInventoryMapper::toDomain)
        .toList();
  }
}
