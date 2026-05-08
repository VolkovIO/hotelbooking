package com.example.hotelbooking.payment.adapter.in.web;

import com.example.hotelbooking.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record PaymentResponse(
    UUID id,
    UUID bookingId,
    UUID userId,
    BigDecimal amount,
    String currency,
    String provider,
    String status,
    String providerPaymentId,
    String failureReason,
    Instant createdAt,
    Instant authorizedAt,
    Instant approvedAt,
    Instant declinedAt,
    Instant cancelledAt,
    Instant updatedAt) {

  static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getId().value(),
        payment.getBookingId().value(),
        payment.getUserId().value(),
        payment.getAmount().value(),
        payment.getCurrency().value(),
        payment.getProvider().name(),
        payment.getStatus().name(),
        providerPaymentId(payment),
        failureReason(payment),
        payment.getCreatedAt(),
        payment.getAuthorizedAt(),
        payment.getApprovedAt(),
        payment.getDeclinedAt(),
        payment.getCancelledAt(),
        payment.getUpdatedAt());
  }

  private static String providerPaymentId(Payment payment) {
    if (payment.getProviderPaymentId() == null) {
      return null;
    }

    return payment.getProviderPaymentId().value();
  }

  private static String failureReason(Payment payment) {
    if (payment.getFailureReason() == null) {
      return null;
    }

    return payment.getFailureReason().value();
  }
}
