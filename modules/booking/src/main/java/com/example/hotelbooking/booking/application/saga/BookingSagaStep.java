package com.example.hotelbooking.booking.application.saga;

/**
 * The next orchestration step that should be executed by the process manager.
 *
 * <p>The step is persisted so the saga can safely continue after application restart or temporary
 * infrastructure failure.
 */
public enum BookingSagaStep {
  HOLD_INVENTORY,
  AUTHORIZE_PAYMENT,
  CONFIRM_BOOKING,
  APPROVE_PAYMENT,
  CANCEL_PAYMENT,
  RELEASE_INVENTORY,
  CANCEL_BOOKING,
  COMPLETE
}
