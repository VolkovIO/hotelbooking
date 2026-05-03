package com.example.hotelbooking.booking.adapter.in.web;

import com.example.hotelbooking.booking.application.command.CancelBookingCommand;
import com.example.hotelbooking.booking.application.command.ConfirmBookingCommand;
import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.port.in.CancelBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.ConfirmBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.CreateBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.GetBookingByIdUseCase;
import com.example.hotelbooking.booking.application.query.GetBookingByIdQuery;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
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

  private final CurrentUserProvider currentUserProvider;
  private final CreateBookingUseCase createBookingUseCase;
  private final GetBookingByIdUseCase getBookingByIdUseCase;
  private final CancelBookingUseCase cancelBookingUseCase;
  private final ConfirmBookingUseCase confirmBookingUseCase;

  @Operation(
      summary = "Create booking",
      description =
          """
          Creates a new booking for the selected hotel, room type, and stay period.

          The booking flow immediately attempts to place an inventory hold for the requested stay period.
          If the hold is created successfully, the booking is stored in ON_HOLD status.
          Payment processing is not part of the current flow.
          """)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
    Booking booking =
        createBookingUseCase.execute(
            new CreateBookingCommand(
                currentUserProvider.currentUser().userId(),
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
    Booking booking =
        getBookingByIdUseCase.execute(
            new GetBookingByIdQuery(
                BookingId.from(bookingId), currentUserProvider.currentUser().userId()));

    return BookingResponse.from(booking);
  }

  @Operation(
      summary = "Cancel booking",
      description =
          """
          Cancels a booking that is currently on hold or already confirmed.

          For an ON_HOLD booking, the associated inventory hold is released before the booking is marked as cancelled.
          For a CONFIRMED booking, the booked inventory rooms are released before the booking is marked as cancelled.
          """)
  @PostMapping("/{bookingId}/cancel")
  public BookingResponse cancel(@PathVariable String bookingId) {
    Booking booking =
        cancelBookingUseCase.execute(
            new CancelBookingCommand(
                BookingId.from(bookingId), currentUserProvider.currentUser().userId()));

    return BookingResponse.from(booking);
  }

  @Operation(
      summary = "Confirm booking",
      description =
          """
          Confirms a booking that is currently on hold.

          The associated inventory hold is finalized before the booking is marked as confirmed.
          """)
  @PostMapping("/{bookingId}/confirm")
  public BookingResponse confirm(@PathVariable String bookingId) {
    Booking booking =
        confirmBookingUseCase.execute(
            new ConfirmBookingCommand(
                BookingId.from(bookingId), currentUserProvider.currentUser().userId()));

    return BookingResponse.from(booking);
  }
}
