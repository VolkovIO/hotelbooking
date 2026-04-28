package com.example.hotelbooking.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.inventory.application.port.in.HotelSummaryResult;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindHotelsServiceTest {

  @Mock private HotelRepository hotelRepository;

  @Test
  void shouldFindHotelsWithDefaultLimitWhenLimitIsNotPositive() {
    FindHotelsService service = new FindHotelsService(hotelRepository);

    when(hotelRepository.findAll(3)).thenReturn(List.of(Hotel.create("Demo Kazan Hotel", "Kazan")));

    List<HotelSummaryResult> result = service.execute(0);

    assertEquals(1, result.size());
    assertEquals("Demo Kazan Hotel", result.getFirst().name());
  }

  @Test
  void shouldLimitRequestedLimitToMaxLimit() {
    FindHotelsService service = new FindHotelsService(hotelRepository);

    when(hotelRepository.findAll(50)).thenReturn(List.of());

    List<HotelSummaryResult> result = service.execute(500);

    assertEquals(0, result.size());
  }
}
