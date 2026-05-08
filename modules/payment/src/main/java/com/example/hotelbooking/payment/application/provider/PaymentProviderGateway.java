package com.example.hotelbooking.payment.application.provider;

import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentProviderPaymentId;

public interface PaymentProviderGateway {

  PaymentProvider provider();

  PaymentProviderAuthorizationResult authorize(PaymentProviderAuthorizationRequest request);

  void approve(PaymentProviderPaymentId providerPaymentId);

  void cancel(PaymentProviderPaymentId providerPaymentId);
}
