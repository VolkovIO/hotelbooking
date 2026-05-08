package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.ApprovePaymentCommand;
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
public class ApprovePaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;

  @Transactional
  public Payment approve(ApprovePaymentCommand command) {
    PaymentId paymentId = new PaymentId(command.paymentId());
    Payment payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

    PaymentProviderGateway gateway = gatewayRegistry.getGateway(payment.getProvider());
    gateway.approve(payment.getProviderPaymentId());

    payment.markApproved();

    return paymentRepository.save(payment);
  }
}
