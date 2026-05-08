package com.example.hotelbooking.payment.application.provider;

import com.example.hotelbooking.payment.domain.PaymentProvider;
import java.io.Serial;

public class PaymentProviderGatewayNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public PaymentProviderGatewayNotFoundException(PaymentProvider provider) {
    super("Payment provider gateway is not configured for provider: " + provider);
  }
}
