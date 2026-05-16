package com.example.hotelbooking.booking.adapter.in.web;

import com.example.hotelbooking.booking.application.command.CancelBookingCommand;
import com.example.hotelbooking.booking.application.command.ConfirmBookingCommand;
import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.port.in.CancelBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.ConfirmBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.CreateBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.GetBookingByIdUseCase;
import com.example.hotelbooking.booking.application.port.in.GetCurrentUserBookingsUseCase;
import com.example.hotelbooking.booking.application.port.in.StartBookingSagaUseCase;
import com.example.hotelbooking.booking.application.query.GetBookingByIdQuery;
import com.example.hotelbooking.booking.application.query.GetCurrentUserBookingsQuery;
import com.example.hotelbooking.booking.application.query.PageCriteria;
import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.StayPeriod;
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
import org.springframework.web.bind.annotation.RequestParam;
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
  private final GetCurrentUserBookingsUseCase getCurrentUserBookingsUseCase;
  private final CancelBookingUseCase cancelBookingUseCase;
  private final ConfirmBookingUseCase confirmBookingUseCase;
  private final StartBookingSagaUseCase startBookingSagaUseCase;

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
      summary = "Get current user bookings",
      description =
          """
          Returns a paged list of bookings owned by the currently authenticated user.

          In the dev security profile the current user is the fixed demo user.
          In the Google JWT profile the current user is resolved from the authenticated Google identity.

          The endpoint is intended for the demo UI "My bookings" page.
          """)
  @GetMapping("/my")
  public BookingPageResponse getMyBookings(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    PagedResult<Booking> result =
        getCurrentUserBookingsUseCase.execute(
            new GetCurrentUserBookingsQuery(
                currentUserProvider.currentUser().userId(), new PageCriteria(page, size)));

    return BookingPageResponse.from(result);
  }

  @Operation(
      summary = "Get booking by id",
      description =
          """
          Returns a booking by its unique identifier.

          If the booking does not exist, the API returns 404 Not Found.
          If the booking belongs to another user, the API returns 403 Forbidden.
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

  @Operation(
      summary = "Start booking saga",
      description =
          """
          Starts an orchestrated booking saga.

          The saga creates a booking, places an inventory hold, authorizes payment,
          confirms the booking, and approves the payment.

          If payment is declined, the saga compensates the already completed steps:
          inventory hold is released and the booking is cancelled.

          This endpoint is intentionally separate from the legacy POST /api/v1/bookings endpoint
          so the saga flow can be tested independently.
          """)
  @PostMapping("/saga")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingSagaResponse startSaga(@Valid @RequestBody StartBookingSagaRequest request) {
    BookingSaga saga =
        startBookingSagaUseCase.execute(
            new StartBookingSagaCommand(
                currentUserProvider.currentUser().userId(),
                request.hotelId(),
                request.roomTypeId(),
                new StayPeriod(request.checkIn(), request.checkOut()),
                request.guestCount(),
                request.paymentAmount(),
                request.paymentCurrency()));

    return BookingSagaResponse.from(saga);
  }
}
