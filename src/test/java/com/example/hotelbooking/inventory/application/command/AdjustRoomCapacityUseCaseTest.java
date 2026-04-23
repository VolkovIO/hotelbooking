package com.example.hotelbooking.inventory.application.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityNotFoundException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdjustRoomCapacityUseCaseTest {

  @Mock private HotelRepository hotelRepository;

  @Mock private RoomAvailabilityRepository roomAvailabilityRepository;

  @InjectMocks private AdjustRoomCapacityUseCase useCase;

  @Test
  void shouldNotAdjustAnyAvailabilityWhenOneDateIsMissing() {
    Hotel hotel = Hotel.create("Demo Hotel", "City");
    hotel.addRoomType("Standart", 2);

    UUID roomTypeId = hotel.getRoomTypes().getFirst().getId();

    LocalDate startDate = LocalDate.of(2030, 6, 10);
    LocalDate endDate = LocalDate.of(2030, 6, 12);

    final RoomAvailabilityPeriodCommand command =
        new RoomAvailabilityPeriodCommand(hotel.getId(), roomTypeId, startDate, endDate, 7);

    when(hotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));

    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, startDate))
        .thenReturn(Optional.of(RoomAvailability.create(hotel.getId(), roomTypeId, startDate, 5)));

    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, startDate.plusDays(1)))
        .thenReturn(Optional.empty());

    assertThrows(RoomAvailabilityNotFoundException.class, () -> useCase.execute(command));

    verify(roomAvailabilityRepository, never()).save(any());
  }
}
