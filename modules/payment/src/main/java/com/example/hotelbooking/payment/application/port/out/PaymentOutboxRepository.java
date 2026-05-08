package com.example.hotelbooking.payment.application.port.out;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PaymentOutboxRepository {

  PaymentOutboxMessage save(PaymentOutboxMessage message);
}
