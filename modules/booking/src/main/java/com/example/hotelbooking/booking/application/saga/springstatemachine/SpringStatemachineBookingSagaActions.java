package com.example.hotelbooking.booking.application.saga.springstatemachine;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.exception.InventoryReservationException;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.payment.PaymentClientException;
import com.example.hotelbooking.booking.application.port.out.BookingObservabilityContext;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaFailureReason;
import com.example.hotelbooking.booking.application.saga.BookingSagaStateException;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.application.saga.action.BookingSagaActionRegistry;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("booking-saga-springstatemachine-prototype")
@RequiredArgsConstructor
class SpringStatemachineBookingSagaActions {

  private final BookingSagaActionRegistry actionRegistry;
  private final BookingSagaRepository sagaRepository;
  private final BookingObservabilityContext observabilityContext;

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> holdInventory() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      authorizePayment() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> confirmBooking() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> approvePayment() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> cancelPayment() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      releaseInventory() {
    return executeAction();
  }

  Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent> cancelBooking() {
    return executeAction();
  }

  Guard<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      paymentAuthorized() {
    return context ->
        SpringStatemachineBookingSagaContext.get(context).getCurrentStep()
            == BookingSagaStep.CONFIRM_BOOKING;
  }

  private Action<SpringStatemachineBookingSagaState, SpringStatemachineBookingSagaEvent>
      executeAction() {
    return context -> {
      BookingSaga saga = SpringStatemachineBookingSagaContext.get(context);

      try (BookingObservabilityContext.ContextScope ignored = observabilityContext.openSaga(saga)) {
        try {
          actionRegistry.execute(saga);
        } catch (RoomHoldFailedException
            | InventoryReservationException
            | PaymentClientException
            | BookingSagaStateException
            | BookingDomainException
            | BookingNotFoundException exception) {
          markFailed(saga, exception);
        }
      }
    };
  }

  private void markFailed(BookingSaga saga, Exception exception) {
    BookingSagaFailureReason reason = new BookingSagaFailureReason(failureMessage(exception));

    saga.markFailed(reason);
    sagaRepository.save(saga);

    if (isExpectedBusinessFailure(exception)) {
      log.warn(
          "Spring Statemachine booking saga prototype action failed: "
              + "sagaId={}, bookingId={}, currentStep={}, errorType={}, reason={}",
          saga.getId().value(),
          saga.getBookingId(),
          saga.getCurrentStep(),
          exception.getClass().getSimpleName(),
          reason.value());

      return;
    }

    log.warn(
        "Spring Statemachine booking saga prototype action failed unexpectedly: "
            + "sagaId={}, bookingId={}, currentStep={}, errorType={}, reason={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getCurrentStep(),
        exception.getClass().getSimpleName(),
        reason.value(),
        exception);
  }

  private boolean isExpectedBusinessFailure(Exception exception) {
    return exception instanceof RoomHoldFailedException
        || exception instanceof BookingDomainException
        || exception instanceof BookingNotFoundException
        || exception instanceof BookingSagaStateException;
  }

  private String failureMessage(Exception exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }

    Throwable cause = exception.getCause();
    if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) {
      return message;
    }

    return message + ": " + cause.getMessage();
  }
}
