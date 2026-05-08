package com.example.hotelbooking.payment.application.exception;

import com.example.hotelbooking.payment.domain.PaymentId;
import java.io.Serial;

public class PaymentNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public PaymentNotFoundException(PaymentId paymentId) {
    super("Payment was not found: " + paymentId.value());
  }
}
