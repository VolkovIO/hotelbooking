package com.example.hotelbooking.inventory.adapter.in.web;

import com.example.hotelbooking.inventory.application.port.in.FindHotelsUseCase;
import com.example.hotelbooking.inventory.application.port.in.GetHotelByIdUseCase;
import com.example.hotelbooking.inventory.application.port.in.GetRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.domain.Hotel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Tag(name = "Inventory Catalog", description = "Public hotel catalog and availability operations")
public class PublicInventoryController {

  private final GetHotelByIdUseCase getHotelByIdUseCase;
  private final GetRoomAvailabilityUseCase getRoomAvailabilityUseCase;
  private final FindHotelsUseCase findHotelsUseCase;

  @GetMapping
  @Operation(
      summary = "Find hotels",
      description =
          """
          Returns public hotel summaries.

          This endpoint is intentionally public.
          A user should be able to browse hotels before starting Google login.
          """)
  public List<HotelSummaryResponse> findHotels(@RequestParam(defaultValue = "3") int limit) {
    return findHotelsUseCase.execute(limit).stream().map(HotelSummaryResponse::from).toList();
  }

  @Operation(
      summary = "Get hotel by id",
      description =
          """
          Returns a hotel with its registered room types.

          This endpoint is intentionally public.
          Booking creation still requires an authenticated user.
          """)
  @GetMapping("/{hotelId}")
  public HotelResponse getHotelById(@PathVariable String hotelId) {
    Hotel hotel = getHotelByIdUseCase.execute(UUID.fromString(hotelId));

    return HotelResponse.from(hotel);
  }

  @Operation(
      summary = "Get room availability",
      description =
          """
          Returns daily room availability for the specified hotel, room type, and date range.

          This endpoint is intentionally public so that users can inspect availability before login.
          """)
  @GetMapping("/{hotelId}/room-types/{roomTypeId}/availability")
  public List<RoomAvailabilityResponse> getRoomAvailability(
      @PathVariable String hotelId,
      @PathVariable String roomTypeId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to) {
    return getRoomAvailabilityUseCase
        .execute(UUID.fromString(hotelId), UUID.fromString(roomTypeId), from, to)
        .stream()
        .map(RoomAvailabilityResponse::from)
        .toList();
  }
}
