package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Command for starting the booking orchestration saga.
 *
 * <p>The command contains both booking data and payment data because the saga must be durable. If
 * the application restarts before payment authorization, the process manager should still know how
 * much money should be authorized and in which currency.
 */
public record StartBookingSagaCommand(
    UserId userId,
    UUID hotelId,
    UUID roomTypeId,
    StayPeriod stayPeriod,
    int guestCount,
    BigDecimal paymentAmount,
    String paymentCurrency) {

  public StartBookingSagaCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(hotelId, "hotelId must not be null");
    Objects.requireNonNull(roomTypeId, "roomTypeId must not be null");
    Objects.requireNonNull(stayPeriod, "stayPeriod must not be null");
    Objects.requireNonNull(paymentAmount, "paymentAmount must not be null");
    Objects.requireNonNull(paymentCurrency, "paymentCurrency must not be null");

    if (guestCount <= 0) {
      throw new IllegalArgumentException("guestCount must be positive");
    }

    if (paymentAmount.signum() <= 0) {
      throw new IllegalArgumentException("paymentAmount must be positive");
    }

    if (paymentCurrency.isBlank()) {
      throw new IllegalArgumentException("paymentCurrency must not be blank");
    }
  }
}
