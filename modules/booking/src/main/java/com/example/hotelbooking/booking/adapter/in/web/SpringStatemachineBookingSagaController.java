package com.example.hotelbooking.booking.adapter.in.web;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.springstatemachine.StartSpringStatemachineBookingSagaUseCase;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
import com.example.hotelbooking.booking.domain.StayPeriod;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("booking-saga-springstatemachine-prototype")
@RequiredArgsConstructor
class SpringStatemachineBookingSagaController {

  private final CurrentUserProvider currentUserProvider;
  private final StartSpringStatemachineBookingSagaUseCase startUseCase;

  @Operation(
      summary = "Start Spring Statemachine booking saga prototype",
      description =
          """
          Starts the experimental Spring Statemachine-based booking saga prototype.

          This endpoint is available only when the
          booking-saga-springstatemachine-prototype profile is enabled.

          The main production-like booking saga endpoint remains POST /api/v1/bookings/saga.
          """)
  @PostMapping("/api/v1/bookings/saga-statemachine")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingSagaResponse startSaga(@Valid @RequestBody StartBookingSagaRequest request) {
    BookingSaga saga =
        startUseCase.execute(
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
