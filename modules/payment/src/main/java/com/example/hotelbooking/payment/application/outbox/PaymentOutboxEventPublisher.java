package com.example.hotelbooking.payment.application.outbox;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PaymentOutboxEventPublisher {

  void publish(PaymentOutboxMessage message) throws PaymentOutboxPublicationException;
}
