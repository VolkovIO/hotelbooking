package com.example.hotelbooking.inventory.api;

import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.command.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.command.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.domain.Hotel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
