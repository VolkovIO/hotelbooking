package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.mongodb.client.result.UpdateResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@Profile("inventory-mongo")
@RequiredArgsConstructor
class MongoRoomAvailabilityRepositoryAdapter implements RoomAvailabilityRepository {

  private final SpringDataMongoRoomAvailabilityRepository repository;
  private final MongoTemplate mongoTemplate;

  private static final String ROOM_AVAILABILITY_ID_PLACEHOLDER = "__ROOM_AVAILABILITY_ID__";
  private static final String ROOMS_PLACEHOLDER = "__ROOMS__";

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

  @Override
  public boolean tryPlaceHold(UUID hotelId, UUID roomTypeId, LocalDate date, int rooms) {
    UpdateResult result =
        mongoTemplate.updateFirst(
            roomAvailabilityWithEnoughAvailableRooms(hotelId, roomTypeId, date, rooms),
            new Update().inc("heldRooms", rooms),
            MongoRoomAvailabilityDocument.class);

    return result.getModifiedCount() == 1;
  }

  @Override
  public boolean releaseHold(UUID hotelId, UUID roomTypeId, LocalDate date, int rooms) {
    UpdateResult result =
        mongoTemplate.updateFirst(
            roomAvailabilityWithEnoughHeldRooms(hotelId, roomTypeId, date, rooms),
            new Update().inc("heldRooms", -rooms),
            MongoRoomAvailabilityDocument.class);

    return result.getModifiedCount() == 1;
  }

  private BasicQuery roomAvailabilityWithEnoughAvailableRooms(
      UUID hotelId, UUID roomTypeId, LocalDate date, int rooms) {
    return new BasicQuery(
        """
        {
          "_id": "__ROOM_AVAILABILITY_ID__",
          "$expr": {
            "$gte": [
              {
                "$subtract": [
                  {
                    "$subtract": [
                      "$totalRooms",
                      "$heldRooms"
                    ]
                  },
                  "$bookedRooms"
                ]
              },
              __ROOMS__
            ]
          }
        }
        """
            .replace(
                ROOM_AVAILABILITY_ID_PLACEHOLDER, roomAvailabilityId(hotelId, roomTypeId, date))
            .replace(ROOMS_PLACEHOLDER, Integer.toString(rooms)));
  }

  private BasicQuery roomAvailabilityWithEnoughHeldRooms(
      UUID hotelId, UUID roomTypeId, LocalDate date, int rooms) {
    return new BasicQuery(
        """
        {
          "_id": "__ROOM_AVAILABILITY_ID__",
          "$expr": {
            "$gte": [
              "$heldRooms",
              __ROOMS__
            ]
          }
        }
        """
            .replace(
                ROOM_AVAILABILITY_ID_PLACEHOLDER, roomAvailabilityId(hotelId, roomTypeId, date))
            .replace(ROOMS_PLACEHOLDER, Integer.toString(rooms)));
  }

  private String roomAvailabilityId(UUID hotelId, UUID roomTypeId, LocalDate date) {
    return MongoRoomAvailabilityDocument.buildId(hotelId, roomTypeId, date);
  }
}
