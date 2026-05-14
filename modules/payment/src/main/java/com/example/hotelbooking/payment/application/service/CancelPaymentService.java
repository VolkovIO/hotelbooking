package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.CancelPaymentCommand;
import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentObservabilityContext;
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
public class CancelPaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;
  private final PaymentStateChangePersistenceService persistenceService;
  private final PaymentObservabilityContext observabilityContext;

  public Payment cancel(CancelPaymentCommand command) {
    PaymentId paymentId = new PaymentId(command.paymentId());
    Payment payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

    try (PaymentObservabilityContext.ContextScope ignored =
        observabilityContext.openPayment(command.correlationId(), payment)) {
      payment.markCancelled();

      PaymentProviderGateway gateway = gatewayRegistry.getGateway(payment.getProvider());
      gateway.cancel(payment.getProviderPaymentId());

      return persistenceService.save(
          payment, PaymentLifecycleEvent.cancelled(payment, command.correlationId(), null));
    }
  }
}
