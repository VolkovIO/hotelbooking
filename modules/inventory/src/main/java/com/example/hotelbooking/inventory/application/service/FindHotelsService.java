package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.port.in.FindHotelsUseCase;
import com.example.hotelbooking.inventory.application.port.in.HotelSummaryResult;
import com.example.hotelbooking.inventory.application.port.in.RoomTypeSummaryResult;
import com.example.hotelbooking.inventory.application.port.out.HotelRepository;
import com.example.hotelbooking.inventory.domain.Hotel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindHotelsService implements FindHotelsUseCase {

  private static final int DEFAULT_LIMIT = 3;
  private static final int MAX_LIMIT = 50;

  private final HotelRepository hotelRepository;

  @Override
  public List<HotelSummaryResult> execute(int limit) {
    int normalizedLimit = normalizeLimit(limit);

    return hotelRepository.findAll(normalizedLimit).stream().map(this::toResult).toList();
  }

  private static int normalizeLimit(int limit) {
    if (limit <= 0) {
      return DEFAULT_LIMIT;
    }

    return Math.min(limit, MAX_LIMIT);
  }

  private HotelSummaryResult toResult(Hotel hotel) {
    return new HotelSummaryResult(
        hotel.getId(),
        hotel.getName(),
        hotel.getCity(),
        hotel.getRoomTypes().stream()
            .map(
                roomType ->
                    new RoomTypeSummaryResult(
                        roomType.getId(), roomType.getName(), roomType.getGuestCapacity()))
            .toList());
  }
}
