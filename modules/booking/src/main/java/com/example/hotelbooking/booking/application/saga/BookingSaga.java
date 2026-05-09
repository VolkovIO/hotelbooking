package com.example.hotelbooking.booking.application.saga;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * BookingSaga stores the technical orchestration state of the booking process.
 *
 * <p>It is not a replacement for the Booking aggregate. The Booking aggregate still owns booking
 * business rules. This class owns process progress: which distributed step was completed and which
 * step should be executed next.
 *
 * <p>The process manager will later use this state to continue the saga after retries, application
 * restarts, or compensation.
 */
@Getter
@SuppressWarnings("PMD.CyclomaticComplexity")
public final class BookingSaga {

  private static final String REASON_MUST_NOT_BE_NULL = "reason must not be null";

  private final BookingSagaId id;
  private final UUID bookingId;
  private BookingSagaStatus status;
  private BookingSagaStep currentStep;
  private UUID paymentId;
  private BookingSagaFailureReason lastFailureReason;
  private int retryCount;
  private Instant nextAttemptAt;
  private final Instant createdAt;
  private Instant completedAt;
  private Instant compensatedAt;
  private Instant failedAt;
  private Instant updatedAt;

  private BookingSaga(
      BookingSagaId id,
      UUID bookingId,
      BookingSagaStatus status,
      BookingSagaStep currentStep,
      UUID paymentId,
      BookingSagaFailureReason lastFailureReason,
      int retryCount,
      Instant nextAttemptAt,
      Instant createdAt,
      Instant completedAt,
      Instant compensatedAt,
      Instant failedAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.bookingId = Objects.requireNonNull(bookingId, "bookingId must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.currentStep = Objects.requireNonNull(currentStep, "currentStep must not be null");
    this.paymentId = paymentId;
    this.lastFailureReason = lastFailureReason;
    this.retryCount = retryCount;
    this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.completedAt = completedAt;
    this.compensatedAt = compensatedAt;
    this.failedAt = failedAt;
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    validateState();
  }

  public static BookingSaga start(UUID bookingId) {
    Instant now = Instant.now();

    return new BookingSaga(
        BookingSagaId.newId(),
        bookingId,
        BookingSagaStatus.STARTED,
        BookingSagaStep.HOLD_INVENTORY,
        null,
        null,
        0,
        now,
        now,
        null,
        null,
        null,
        now);
  }

  public static BookingSaga restore(
      BookingSagaId id,
      UUID bookingId,
      BookingSagaStatus status,
      BookingSagaStep currentStep,
      UUID paymentId,
      BookingSagaFailureReason lastFailureReason,
      int retryCount,
      Instant nextAttemptAt,
      Instant createdAt,
      Instant completedAt,
      Instant compensatedAt,
      Instant failedAt,
      Instant updatedAt) {
    return new BookingSaga(
        id,
        bookingId,
        status,
        currentStep,
        paymentId,
        lastFailureReason,
        retryCount,
        nextAttemptAt,
        createdAt,
        completedAt,
        compensatedAt,
        failedAt,
        updatedAt);
  }

  public void markInventoryHeld() {
    requireExecutableState("mark inventory held");

    this.status = BookingSagaStatus.IN_PROGRESS;
    this.currentStep = BookingSagaStep.AUTHORIZE_PAYMENT;
    this.updatedAt = Instant.now();
  }

  public void markPaymentAuthorized(UUID paymentId) {
    requireExecutableState("mark payment authorized");

    this.status = BookingSagaStatus.IN_PROGRESS;
    this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
    this.currentStep = BookingSagaStep.CONFIRM_BOOKING;
    this.updatedAt = Instant.now();
  }

  public void markPaymentDeclined(BookingSagaFailureReason reason) {
    requireExecutableState("mark payment declined");

    this.status = BookingSagaStatus.COMPENSATING;
    this.lastFailureReason = Objects.requireNonNull(reason, REASON_MUST_NOT_BE_NULL);
    this.currentStep = BookingSagaStep.RELEASE_INVENTORY;
    this.updatedAt = Instant.now();
  }

  public void markBookingConfirmed() {
    requireExecutableState("mark booking confirmed");

    this.status = BookingSagaStatus.IN_PROGRESS;
    this.currentStep = BookingSagaStep.APPROVE_PAYMENT;
    this.updatedAt = Instant.now();
  }

  public void markPaymentApproved() {
    requireExecutableState("mark payment approved");

    this.status = BookingSagaStatus.COMPLETED;
    this.currentStep = BookingSagaStep.COMPLETE;
    this.completedAt = Instant.now();
    this.updatedAt = this.completedAt;
  }

  public void startCompensation(BookingSagaFailureReason reason) {
    if (isFinished()) {
      throw new BookingSagaStateException("Cannot compensate finished booking saga");
    }

    this.status = BookingSagaStatus.COMPENSATING;
    this.lastFailureReason = Objects.requireNonNull(reason, REASON_MUST_NOT_BE_NULL);
    this.currentStep = compensationStep();
    this.updatedAt = Instant.now();
  }

  public void markPaymentCancelled() {
    requireCompensatingState("mark payment cancelled");

    this.currentStep = BookingSagaStep.RELEASE_INVENTORY;
    this.updatedAt = Instant.now();
  }

  public void markInventoryReleased() {
    requireCompensatingState("mark inventory released");

    this.currentStep = BookingSagaStep.CANCEL_BOOKING;
    this.updatedAt = Instant.now();
  }

  public void markBookingCancelled() {
    requireCompensatingState("mark booking cancelled");

    this.status = BookingSagaStatus.COMPENSATED;
    this.currentStep = BookingSagaStep.COMPLETE;
    this.compensatedAt = Instant.now();
    this.updatedAt = this.compensatedAt;
  }

  public void scheduleRetry(BookingSagaFailureReason reason, Instant nextAttemptAt) {
    if (isFinished()) {
      throw new BookingSagaStateException("Cannot retry finished booking saga");
    }

    this.status = BookingSagaStatus.WAITING_RETRY;
    this.lastFailureReason = Objects.requireNonNull(reason, REASON_MUST_NOT_BE_NULL);
    this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    this.retryCount++;
    this.updatedAt = Instant.now();
  }

  public void resumeAfterRetry() {
    if (status != BookingSagaStatus.WAITING_RETRY) {
      throw new BookingSagaStateException("Only waiting retry saga can be resumed");
    }

    if (isCompensationStep(currentStep)) {
      this.status = BookingSagaStatus.COMPENSATING;
    } else {
      this.status = BookingSagaStatus.IN_PROGRESS;
    }

    this.updatedAt = Instant.now();
  }

  public void markFailed(BookingSagaFailureReason reason) {
    if (isFinished()) {
      throw new BookingSagaStateException("Cannot fail finished booking saga");
    }

    this.status = BookingSagaStatus.FAILED;
    this.lastFailureReason = Objects.requireNonNull(reason, REASON_MUST_NOT_BE_NULL);
    this.failedAt = Instant.now();
    this.updatedAt = this.failedAt;
  }

  public boolean isFinished() {
    return status == BookingSagaStatus.COMPLETED
        || status == BookingSagaStatus.COMPENSATED
        || status == BookingSagaStatus.FAILED;
  }

  public boolean isWaitingRetry() {
    return status == BookingSagaStatus.WAITING_RETRY;
  }

  public boolean isCompensating() {
    return status == BookingSagaStatus.COMPENSATING;
  }

  private BookingSagaStep compensationStep() {
    if (paymentId != null) {
      return BookingSagaStep.CANCEL_PAYMENT;
    }

    return BookingSagaStep.RELEASE_INVENTORY;
  }

  private void requireExecutableState(String operation) {
    if (status != BookingSagaStatus.STARTED
        && status != BookingSagaStatus.IN_PROGRESS
        && status != BookingSagaStatus.WAITING_RETRY) {
      throw new BookingSagaStateException(
          "Cannot " + operation + " for booking saga with status " + status);
    }
  }

  private void requireCompensatingState(String operation) {
    if (status != BookingSagaStatus.COMPENSATING && status != BookingSagaStatus.WAITING_RETRY) {
      throw new BookingSagaStateException(
          "Cannot " + operation + " for booking saga with status " + status);
    }
  }

  private boolean isCompensationStep(BookingSagaStep step) {
    return step == BookingSagaStep.CANCEL_PAYMENT
        || step == BookingSagaStep.RELEASE_INVENTORY
        || step == BookingSagaStep.CANCEL_BOOKING;
  }

  private void validateState() {
    if (retryCount < 0) {
      throw new BookingSagaStateException("retryCount must not be negative");
    }

    if (status == BookingSagaStatus.COMPLETED && completedAt == null) {
      throw new BookingSagaStateException("completed saga must have completedAt");
    }

    if (status == BookingSagaStatus.COMPENSATED && compensatedAt == null) {
      throw new BookingSagaStateException("compensated saga must have compensatedAt");
    }

    if (status == BookingSagaStatus.FAILED && failedAt == null) {
      throw new BookingSagaStateException("failed saga must have failedAt");
    }
  }
}
