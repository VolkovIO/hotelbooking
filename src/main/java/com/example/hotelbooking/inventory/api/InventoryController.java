package com.example.hotelbooking.inventory.api;

import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.command.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.command.AdjustRoomCapacityUseCase;
import com.example.hotelbooking.inventory.application.command.InitializeRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.command.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;
import com.example.hotelbooking.inventory.application.query.GetHotelByIdUseCase;
import com.example.hotelbooking.inventory.application.query.GetRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.domain.Hotel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/hotels")
@RequiredArgsConstructor
@Tag(name = "Inventory Admin", description = "Administrative operations for hotel inventory")
public class InventoryController {

  private final RegisterHotelUseCase registerHotelUseCase;
  private final AddRoomTypeUseCase addRoomTypeUseCase;
  private final GetHotelByIdUseCase getHotelByIdUseCase;
  private final InitializeRoomAvailabilityUseCase initializeRoomAvailabilityUseCase;
  private final AdjustRoomCapacityUseCase adjustRoomCapacityUseCase;
  private final GetRoomAvailabilityUseCase getRoomAvailabilityUseCase;

  @Operation(
      summary = "Register hotel",
      description =
          """
          Registers a new hotel in the inventory context.

          The hotel is created without room types. Room types can be added in a separate operation.
          """)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public HotelResponse registerHotel(@Valid @RequestBody RegisterHotelRequest request) {
    Hotel hotel =
        registerHotelUseCase.execute(new RegisterHotelCommand(request.name(), request.city()));
    return HotelResponse.from(hotel);
  }

  @Operation(
      summary = "Add room type",
      description =
          """
          Adds a room type to an existing hotel.

          Room type names must be unique within the same hotel.
          """)
  @PostMapping("/{hotelId}/room-types")
  @ResponseStatus(HttpStatus.CREATED)
  public HotelResponse addRoomType(
      @PathVariable String hotelId, @Valid @RequestBody AddRoomTypeRequest request) {
    Hotel hotel =
        addRoomTypeUseCase.execute(
            new AddRoomTypeCommand(
                UUID.fromString(hotelId), request.name(), request.guestCapacity()));

    return HotelResponse.from(hotel);
  }

  @Operation(
      summary = "Get hotel by id",
      description =
          """
          Returns a hotel with its registered room types.

          If the hotel does not exist, the API returns 404 Not Found.
          """)
  @GetMapping("/{hotelId}")
  public HotelResponse getHotelById(@PathVariable String hotelId) {
    Hotel hotel = getHotelByIdUseCase.execute(UUID.fromString(hotelId));
    return HotelResponse.from(hotel);
  }

  @Operation(
      summary = "Initialize room availability",
      description =
          """
          Initializes daily room availability for the specified hotel and room type within the given date range.

          This operation is intended for dates where availability does not yet exist.
          If availability already exists for any date in the range, the API should reject the request.
          """)
  @PostMapping("/{hotelId}/room-types/{roomTypeId}/availability/initialization")
  @ResponseStatus(HttpStatus.CREATED)
  public void initializeRoomAvailability(
      @PathVariable String hotelId,
      @PathVariable String roomTypeId,
      @Valid @RequestBody SetRoomAvailabilityRequest request) {
    initializeRoomAvailabilityUseCase.execute(
        new RoomAvailabilityPeriodCommand(
            UUID.fromString(hotelId),
            UUID.fromString(roomTypeId),
            request.from(),
            request.to(),
            request.totalRooms()));
  }

  @Operation(
      summary = "Adjust room capacity",
      description =
          """
          Adjusts total room capacity for existing daily availability records within the given date range.

          This operation keeps existing held and booked rooms unchanged.
          If availability does not exist for any date in the range, the API should reject the request.
          """)
  @PutMapping("/{hotelId}/room-types/{roomTypeId}/availability/capacity")
  @ResponseStatus(HttpStatus.OK)
  public void adjustRoomCapacity(
      @PathVariable String hotelId,
      @PathVariable String roomTypeId,
      @Valid @RequestBody SetRoomAvailabilityRequest request) {
    adjustRoomCapacityUseCase.execute(
        new RoomAvailabilityPeriodCommand(
            UUID.fromString(hotelId),
            UUID.fromString(roomTypeId),
            request.from(),
            request.to(),
            request.totalRooms()));
  }

  @Operation(
      summary = "Get room availability",
      description =
          """
          Returns daily room availability for the specified hotel, room type, and date range.
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
