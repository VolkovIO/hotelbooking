package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.CancelPaymentCommand;
import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGateway;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGatewayRegistry;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelPaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;

  @Transactional
  public Payment cancel(CancelPaymentCommand command) {
    PaymentId paymentId = new PaymentId(command.paymentId());
    Payment payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

    PaymentProviderGateway gateway = gatewayRegistry.getGateway(payment.getProvider());
    gateway.cancel(payment.getProviderPaymentId());

    payment.markCancelled();

    return paymentRepository.save(payment);
  }
}
