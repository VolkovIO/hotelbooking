package com.example.hotelbooking.booking.application.saga;

/**
 * Technical status of the booking saga process.
 *
 * <p>This is intentionally separated from the Booking aggregate status. Booking status describes
 * the business state of a booking, while saga status describes orchestration progress.
 */
public enum BookingSagaStatus {
  STARTED,
  IN_PROGRESS,
  WAITING_RETRY,
  COMPENSATING,
  COMPLETED,
  COMPENSATED,
  FAILED
}
