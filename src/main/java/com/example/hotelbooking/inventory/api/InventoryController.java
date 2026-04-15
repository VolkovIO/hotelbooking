package com.example.hotelbooking.inventory.api;

import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.command.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.command.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.application.command.SetRoomAvailabilityCommand;
import com.example.hotelbooking.inventory.application.command.SetRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.query.GetHotelByIdUseCase;
import com.example.hotelbooking.inventory.domain.Hotel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
  private final SetRoomAvailabilityUseCase setRoomAvailabilityUseCase;

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
                java.util.UUID.fromString(hotelId), request.name(), request.guestCapacity()));

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
    Hotel hotel = getHotelByIdUseCase.execute(java.util.UUID.fromString(hotelId));
    return HotelResponse.from(hotel);
  }

  @Operation(
      summary = "Set room availability",
      description =
          """
          Sets daily room availability for the specified hotel and room type within the given date range.

          The range is inclusive on both ends.
          """)
  @PutMapping("/{hotelId}/room-types/{roomTypeId}/availability")
  @ResponseStatus(HttpStatus.OK)
  public void setRoomAvailability(
      @PathVariable String hotelId,
      @PathVariable String roomTypeId,
      @Valid @RequestBody SetRoomAvailabilityRequest request) {
    setRoomAvailabilityUseCase.execute(
        new SetRoomAvailabilityCommand(
            java.util.UUID.fromString(hotelId),
            java.util.UUID.fromString(roomTypeId),
            request.from(),
            request.to(),
            request.totalRooms()));
  }
}
