package com.example.hotelbooking.payment.application.provider;

import com.example.hotelbooking.payment.domain.PaymentFailureReason;
import com.example.hotelbooking.payment.domain.PaymentProviderPaymentId;
import java.util.Objects;

public record PaymentProviderAuthorizationResult(
    boolean authorized,
    PaymentProviderPaymentId providerPaymentId,
    PaymentFailureReason failureReason) {

  public PaymentProviderAuthorizationResult {
    if (authorized) {
      Objects.requireNonNull(providerPaymentId, "providerPaymentId must not be null");
      if (failureReason != null) {
        throw new IllegalArgumentException("authorized result must not have failure reason");
      }
    } else {
      Objects.requireNonNull(failureReason, "failureReason must not be null");
      if (providerPaymentId != null) {
        throw new IllegalArgumentException("declined result must not have provider payment id");
      }
    }
  }

  public static PaymentProviderAuthorizationResult authorized(
      PaymentProviderPaymentId providerPaymentId) {
    return new PaymentProviderAuthorizationResult(true, providerPaymentId, null);
  }

  public static PaymentProviderAuthorizationResult declined(PaymentFailureReason failureReason) {
    return new PaymentProviderAuthorizationResult(false, null, failureReason);
  }
}
