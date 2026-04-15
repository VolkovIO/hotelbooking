package com.example.hotelbooking.booking.api;

import com.example.hotelbooking.booking.application.CreateBookingCommand;
import com.example.hotelbooking.booking.application.CreateBookingUseCase;
import com.example.hotelbooking.booking.application.GetBookingByIdUseCase;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Operations for creating and managing hotel bookings")
public class BookingController {

  private final CreateBookingUseCase createBookingUseCase;
  private final GetBookingByIdUseCase getBookingByIdUseCase;

  @Operation(
      summary = "Create booking",
      description =
          """
      Creates a new booking draft for the selected hotel, room type, and stay period.

      The booking is created in the initial domain state and stored in the current persistence profile.
      At this stage, the flow does not yet include inventory hold or payment processing.
      """)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
    Booking booking =
        createBookingUseCase.execute(
            new CreateBookingCommand(
                request.hotelId(),
                request.roomTypeId(),
                request.checkIn(),
                request.checkOut(),
                request.guestCount()));

    return BookingResponse.from(booking);
  }

  @Operation(
      summary = "Get booking by id",
      description =
          """
          Returns a booking by its unique identifier.

          If the booking does not exist, the API returns 404 Not Found.
          """)
  @GetMapping("/{bookingId}")
  public BookingResponse getById(@PathVariable String bookingId) {
    Booking booking = getBookingByIdUseCase.execute(BookingId.from(bookingId));
    return BookingResponse.from(booking);
  }
}
