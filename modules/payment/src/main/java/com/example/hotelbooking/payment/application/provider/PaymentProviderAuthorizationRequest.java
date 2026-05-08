package com.example.hotelbooking.payment.application.provider;

import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.PaymentAmount;
import com.example.hotelbooking.payment.domain.PaymentCurrency;
import com.example.hotelbooking.payment.domain.PaymentUserId;
import java.util.Objects;

public record PaymentProviderAuthorizationRequest(
    BookingId bookingId, PaymentUserId userId, PaymentAmount amount, PaymentCurrency currency) {

  public PaymentProviderAuthorizationRequest {
    Objects.requireNonNull(bookingId, "bookingId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
  }
}
