package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.outbox.PaymentOutboxMessage;
import com.example.hotelbooking.payment.application.port.out.PaymentOutboxRepository;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.event.PaymentLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentStateChangePersistenceService {

  private final PaymentRepository paymentRepository;
  private final PaymentOutboxRepository paymentOutboxRepository;

  @Transactional
  public Payment save(Payment payment, PaymentLifecycleEvent event) {
    Payment savedPayment = paymentRepository.save(payment);
    paymentOutboxRepository.save(PaymentOutboxMessage.from(event));

    return savedPayment;
  }
}
