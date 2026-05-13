package com.example.hotelbooking.payment.application.command;

import java.util.Objects;
import java.util.UUID;

public record CancelPaymentCommand(UUID paymentId, UUID correlationId) {

  public CancelPaymentCommand {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
  }
}
