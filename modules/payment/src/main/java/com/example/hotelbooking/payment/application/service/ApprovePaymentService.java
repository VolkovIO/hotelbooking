package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.ApprovePaymentCommand;
import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGateway;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGatewayRegistry;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import com.example.hotelbooking.payment.domain.event.PaymentLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovePaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;
  private final PaymentStateChangePersistenceService persistenceService;

  public Payment approve(ApprovePaymentCommand command) {
    PaymentId paymentId = new PaymentId(command.paymentId());
    Payment payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

    payment.markApproved();

    PaymentProviderGateway gateway = gatewayRegistry.getGateway(payment.getProvider());
    gateway.approve(payment.getProviderPaymentId());

    return persistenceService.save(
        payment, PaymentLifecycleEvent.approved(payment, command.correlationId(), null));
  }
}
