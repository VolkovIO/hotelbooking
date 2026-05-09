package com.example.hotelbooking.booking.application.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingSagaTest {

  private static final UUID BOOKING_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID PAYMENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final BookingSagaFailureReason PAYMENT_DECLINED_REASON =
      new BookingSagaFailureReason("payment declined");
  private static final BookingSagaFailureReason TECHNICAL_FAILURE_REASON =
      new BookingSagaFailureReason("temporary infrastructure failure");

  @Test
  void shouldStartSagaWithHoldInventoryStep() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);

    assertEquals(BOOKING_ID, saga.getBookingId());
    assertEquals(BookingSagaStatus.STARTED, saga.getStatus());
    assertEquals(BookingSagaStep.HOLD_INVENTORY, saga.getCurrentStep());
    assertNull(saga.getPaymentId());
    assertEquals(0, saga.getRetryCount());
    assertNotNull(saga.getNextAttemptAt());
  }

  @Test
  void shouldMoveThroughHappyPath() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);

    saga.markInventoryHeld();
    assertEquals(BookingSagaStatus.IN_PROGRESS, saga.getStatus());
    assertEquals(BookingSagaStep.AUTHORIZE_PAYMENT, saga.getCurrentStep());

    saga.markPaymentAuthorized(PAYMENT_ID);
    assertEquals(PAYMENT_ID, saga.getPaymentId());
    assertEquals(BookingSagaStep.CONFIRM_BOOKING, saga.getCurrentStep());

    saga.markBookingConfirmed();
    assertEquals(BookingSagaStep.APPROVE_PAYMENT, saga.getCurrentStep());

    saga.markPaymentApproved();
    assertEquals(BookingSagaStatus.COMPLETED, saga.getStatus());
    assertEquals(BookingSagaStep.COMPLETE, saga.getCurrentStep());
    assertNotNull(saga.getCompletedAt());
    assertTrue(saga.isFinished());
  }

  @Test
  void shouldStartCompensationFromPaymentDeclined() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);

    saga.markInventoryHeld();
    saga.markPaymentDeclined(PAYMENT_DECLINED_REASON);

    assertEquals(BookingSagaStatus.COMPENSATING, saga.getStatus());
    assertEquals(BookingSagaStep.RELEASE_INVENTORY, saga.getCurrentStep());
    assertEquals(PAYMENT_DECLINED_REASON, saga.getLastFailureReason());
  }

  @Test
  void shouldCompensateAfterPaymentWasAuthorized() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);

    saga.markInventoryHeld();
    saga.markPaymentAuthorized(PAYMENT_ID);
    saga.startCompensation(TECHNICAL_FAILURE_REASON);

    assertEquals(BookingSagaStatus.COMPENSATING, saga.getStatus());
    assertEquals(BookingSagaStep.CANCEL_PAYMENT, saga.getCurrentStep());

    saga.markPaymentCancelled();
    assertEquals(BookingSagaStep.RELEASE_INVENTORY, saga.getCurrentStep());

    saga.markInventoryReleased();
    assertEquals(BookingSagaStep.CANCEL_BOOKING, saga.getCurrentStep());

    saga.markBookingCancelled();
    assertEquals(BookingSagaStatus.COMPENSATED, saga.getStatus());
    assertEquals(BookingSagaStep.COMPLETE, saga.getCurrentStep());
    assertNotNull(saga.getCompensatedAt());
    assertTrue(saga.isFinished());
  }

  @Test
  void shouldScheduleRetryWithoutLosingCurrentStep() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);
    Instant nextAttemptAt = Instant.now().plusSeconds(30);

    saga.markInventoryHeld();
    saga.scheduleRetry(TECHNICAL_FAILURE_REASON, nextAttemptAt);

    assertEquals(BookingSagaStatus.WAITING_RETRY, saga.getStatus());
    assertEquals(BookingSagaStep.AUTHORIZE_PAYMENT, saga.getCurrentStep());
    assertEquals(1, saga.getRetryCount());
    assertEquals(nextAttemptAt, saga.getNextAttemptAt());

    saga.resumeAfterRetry();

    assertEquals(BookingSagaStatus.IN_PROGRESS, saga.getStatus());
    assertEquals(BookingSagaStep.AUTHORIZE_PAYMENT, saga.getCurrentStep());
  }

  @Test
  void shouldResumeCompensationAfterRetry() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);
    Instant nextAttemptAt = Instant.now().plusSeconds(30);

    saga.markInventoryHeld();
    saga.markPaymentAuthorized(PAYMENT_ID);
    saga.startCompensation(TECHNICAL_FAILURE_REASON);
    saga.scheduleRetry(TECHNICAL_FAILURE_REASON, nextAttemptAt);

    assertEquals(BookingSagaStatus.WAITING_RETRY, saga.getStatus());
    assertEquals(BookingSagaStep.CANCEL_PAYMENT, saga.getCurrentStep());

    saga.resumeAfterRetry();

    assertEquals(BookingSagaStatus.COMPENSATING, saga.getStatus());
    assertEquals(BookingSagaStep.CANCEL_PAYMENT, saga.getCurrentStep());
  }

  @Test
  void shouldRejectChangingFinishedSaga() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);

    saga.markInventoryHeld();
    saga.markPaymentAuthorized(PAYMENT_ID);
    saga.markBookingConfirmed();
    saga.markPaymentApproved();

    assertThrows(
        BookingSagaStateException.class, () -> saga.startCompensation(TECHNICAL_FAILURE_REASON));
  }

  @Test
  void shouldMarkSagaAsFailed() {
    BookingSaga saga = BookingSaga.start(BOOKING_ID);

    saga.markFailed(TECHNICAL_FAILURE_REASON);

    assertEquals(BookingSagaStatus.FAILED, saga.getStatus());
    assertEquals(TECHNICAL_FAILURE_REASON, saga.getLastFailureReason());
    assertNotNull(saga.getFailedAt());
    assertTrue(saga.isFinished());
  }
}
