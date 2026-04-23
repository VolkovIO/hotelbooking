package com.example.hotelbooking.inventory.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.inventory.application.exception.RoomAvailabilityAlreadyExistsException;
import com.example.hotelbooking.inventory.application.port.HotelRepository;
import com.example.hotelbooking.inventory.application.port.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializeRoomAvailabilityUseCaseTest {

  @Mock private HotelRepository hotelRepository;
  @Mock private RoomAvailabilityRepository roomAvailabilityRepository;

  @InjectMocks private InitializeRoomAvailabilityUseCase useCase;

  private Hotel hotel;
  private UUID roomTypeId;
  private LocalDate startDate;
  private LocalDate endDate;
  private RoomAvailabilityPeriodCommand command;

  @BeforeEach
  void setUp() {
    hotel = Hotel.create("Demo Hotel", "City");
    hotel.addRoomType("Standart", 2);

    roomTypeId = hotel.getRoomTypes().getFirst().getId();
    startDate = LocalDate.of(2030, 6, 10);
    endDate = LocalDate.of(2030, 6, 12);

    command = new RoomAvailabilityPeriodCommand(hotel.getId(), roomTypeId, startDate, endDate, 5);
  }

  @Test
  void shouldNotCreateAnyAvailabilityWhenOneDateAlreadyExists() {

    when(hotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));

    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, startDate))
        .thenReturn(Optional.empty());

    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, startDate.plusDays(1)))
        .thenReturn(
            Optional.of(
                RoomAvailability.create(
                    hotel.getId(), roomTypeId, startDate.plusDays(1), command.totalRooms())));

    assertThrows(RoomAvailabilityAlreadyExistsException.class, () -> useCase.execute(command));

    verify(roomAvailabilityRepository, never()).save(any());
  }

  @Test
  void shouldCreateAvailabilityForEntireRangeWhenAllDatesAreFree() {
    when(hotelRepository.findById(hotel.getId())).thenReturn(Optional.of(hotel));
    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, startDate))
        .thenReturn(Optional.empty());
    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, startDate.plusDays(1)))
        .thenReturn(Optional.empty());
    when(roomAvailabilityRepository.findByHotelIdAndRoomTypeIdAndDate(
            hotel.getId(), roomTypeId, endDate))
        .thenReturn(Optional.empty());

    useCase.execute(command);

    ArgumentCaptor<RoomAvailability> captor = ArgumentCaptor.forClass(RoomAvailability.class);

    verify(roomAvailabilityRepository, times(3)).save(captor.capture());

    List<RoomAvailability> saved = captor.getAllValues();

    assertEquals(3, saved.size());

    assertEquals(hotel.getId(), saved.getFirst().getHotelId());
    assertEquals(roomTypeId, saved.getFirst().getRoomTypeId());
    assertEquals(startDate, saved.get(0).getDate());
    assertEquals(5, saved.get(0).getTotalRooms());

    assertEquals(startDate.plusDays(1), saved.get(1).getDate());
    assertEquals(5, saved.get(1).getTotalRooms());

    assertEquals(endDate, saved.get(2).getDate());
    assertEquals(5, saved.get(2).getTotalRooms());
  }
}
