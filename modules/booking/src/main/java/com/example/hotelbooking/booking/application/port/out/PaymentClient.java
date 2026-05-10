package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.payment.PaymentAuthorizationRequest;
import com.example.hotelbooking.booking.application.payment.PaymentResult;
import java.util.UUID;

public interface PaymentClient {

  PaymentResult authorize(PaymentAuthorizationRequest request);

  PaymentResult approve(UUID paymentId);

  PaymentResult cancel(UUID paymentId);
}
