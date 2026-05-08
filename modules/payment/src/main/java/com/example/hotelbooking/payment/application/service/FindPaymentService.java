package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindPaymentService {

  private final PaymentRepository paymentRepository;

  @Transactional(readOnly = true)
  public Payment findById(UUID paymentIdValue) {
    PaymentId paymentId = new PaymentId(paymentIdValue);

    return paymentRepository
        .findById(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException(paymentId));
  }
}
