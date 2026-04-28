package com.example.hotelbooking.inventory.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface RoomAvailabilityRepositoryContractTest {

  RoomAvailabilityRepository repository();

  @Test
  default void shouldSaveAndFindAvailabilityByDate() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2030, 6, 10);

    RoomAvailability availability = RoomAvailability.create(hotelId, roomTypeId, date, 10);

    repository().save(availability);

    assertTrue(
        repository().findByHotelIdAndRoomTypeIdAndDate(hotelId, roomTypeId, date).isPresent());
  }

  @Test
  default void shouldSaveAndFindAvailabilityByDateRange() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();

    RoomAvailability first =
        RoomAvailability.create(hotelId, roomTypeId, LocalDate.of(2030, 6, 10), 10);

    RoomAvailability second =
        RoomAvailability.create(hotelId, roomTypeId, LocalDate.of(2030, 6, 11), 10);

    repository().saveAll(List.of(first, second));

    List<RoomAvailability> result =
        repository()
            .findByRoomTypeAndDateRange(
                hotelId, roomTypeId, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11));

    assertEquals(2, result.size());
  }
}
