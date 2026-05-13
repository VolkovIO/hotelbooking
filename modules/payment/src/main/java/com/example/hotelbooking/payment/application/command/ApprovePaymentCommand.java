package com.example.hotelbooking.payment.application.command;

import java.util.Objects;
import java.util.UUID;

public record ApprovePaymentCommand(UUID paymentId, UUID correlationId) {

  public ApprovePaymentCommand {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
  }
}
