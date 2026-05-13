package com.example.hotelbooking.booking.application.saga;

import com.example.hotelbooking.booking.application.exception.InventoryReservationException;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.payment.PaymentClientException;
import com.example.hotelbooking.booking.application.port.out.BookingObservabilityContext;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.saga.action.BookingSagaActionRegistry;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.CyclomaticComplexity")
public class BookingSagaProcessManager {

  private static final int MAX_STEPS_PER_RUN = 20;

  private final BookingSagaRepository sagaRepository;
  private final BookingSagaActionRegistry actionRegistry;
  private final BookingSagaRetryProperties retryProperties;
  private final BookingObservabilityContext observabilityContext;

  public BookingSaga process(BookingSagaId sagaId) {
    Objects.requireNonNull(sagaId, "sagaId must not be null");

    BookingSaga saga =
        sagaRepository.findById(sagaId).orElseThrow(() -> new BookingSagaNotFoundException(sagaId));

    try (BookingObservabilityContext.ContextScope ignored = observabilityContext.openSaga(saga)) {
      return processWithContext(saga);
    }
  }

  private BookingSaga processWithContext(BookingSaga saga) {
    BookingSaga currentSaga = saga;

    log.info(
        "Processing booking saga: sagaId={}, bookingId={}, status={}, currentStep={}",
        currentSaga.getId().value(),
        currentSaga.getBookingId(),
        currentSaga.getStatus(),
        currentSaga.getCurrentStep());

    if (currentSaga.isWaitingRetry()) {
      currentSaga.resumeAfterRetry();
      sagaRepository.save(currentSaga);
    }

    int executedSteps = 0;

    while (!currentSaga.isFinished() && currentSaga.getCurrentStep() != BookingSagaStep.COMPLETE) {
      if (executedSteps >= MAX_STEPS_PER_RUN) {
        throw new BookingSagaStateException(
            "Booking saga exceeded maximum steps per run: sagaId=" + currentSaga.getId().value());
      }

      try {
        currentSaga = actionRegistry.execute(currentSaga);
      } catch (PaymentClientException | InventoryReservationException exception) {
        handleRetryableStepFailure(currentSaga, exception);

        if (currentSaga.isWaitingRetry() || currentSaga.isFinished()) {
          return currentSaga;
        }
      } catch (RoomHoldFailedException | BookingDomainException exception) {
        markNonRetryableStepFailure(currentSaga, exception);
        return currentSaga;
      }

      executedSteps++;
    }

    log.info(
        "Booking saga processing finished: sagaId={}, bookingId={}, status={}, currentStep={}",
        currentSaga.getId().value(),
        currentSaga.getBookingId(),
        currentSaga.getStatus(),
        currentSaga.getCurrentStep());

    return currentSaga;
  }

  private void handleRetryableStepFailure(BookingSaga saga, RuntimeException exception) {
    BookingSagaFailureReason reason = failureReason(exception);

    if (canRetry(saga)) {
      Instant nextAttemptAt = Instant.now().plus(retryProperties.getDelay());

      saga.scheduleRetry(reason, nextAttemptAt);
      sagaRepository.save(saga);

      log.warn(
          "Booking saga step failed, retry scheduled: "
              + "sagaId={}, bookingId={}, currentStep={}, retryCount={}, nextAttemptAt={}, reason={}",
          saga.getId().value(),
          saga.getBookingId(),
          saga.getCurrentStep(),
          saga.getRetryCount(),
          saga.getNextAttemptAt(),
          reason.value(),
          exception);

      return;
    }

    if (shouldCompensateAfterRetriesExhausted(saga)) {
      saga.startCompensation(reason);
      sagaRepository.save(saga);

      log.warn(
          "Booking saga retries exhausted, starting compensation: "
              + "sagaId={}, bookingId={}, currentStep={}, retryCount={}, reason={}",
          saga.getId().value(),
          saga.getBookingId(),
          saga.getCurrentStep(),
          saga.getRetryCount(),
          reason.value(),
          exception);

      return;
    }

    saga.markFailed(reason);
    sagaRepository.save(saga);

    log.warn(
        "Booking saga retries exhausted, marking saga as failed: "
            + "sagaId={}, bookingId={}, currentStep={}, retryCount={}, reason={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getCurrentStep(),
        saga.getRetryCount(),
        reason.value(),
        exception);
  }

  private void markNonRetryableStepFailure(BookingSaga saga, RuntimeException exception) {
    BookingSagaFailureReason reason = failureReason(exception);

    saga.markFailed(reason);
    sagaRepository.save(saga);

    log.warn(
        "Booking saga step failed without retry: "
            + "sagaId={}, bookingId={}, currentStep={}, errorType={}, reason={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getCurrentStep(),
        exception.getClass().getSimpleName(),
        reason.value());
  }

  private boolean canRetry(BookingSaga saga) {
    return retryProperties.isEnabled() && saga.getRetryCount() < retryProperties.getMaxRetries();
  }

  private boolean shouldCompensateAfterRetriesExhausted(BookingSaga saga) {
    return !saga.isCompensating()
        && !isCompensationStep(saga.getCurrentStep())
        && saga.getCurrentStep() != BookingSagaStep.HOLD_INVENTORY;
  }

  private boolean isCompensationStep(BookingSagaStep step) {
    return step == BookingSagaStep.CANCEL_PAYMENT
        || step == BookingSagaStep.RELEASE_INVENTORY
        || step == BookingSagaStep.CANCEL_BOOKING;
  }

  private BookingSagaFailureReason failureReason(RuntimeException exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }

    Throwable cause = exception.getCause();
    if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) {
      return new BookingSagaFailureReason(message);
    }

    return new BookingSagaFailureReason(message + ": " + cause.getMessage());
  }
}
