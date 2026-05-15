package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.ApprovePaymentCommand;
import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentMetrics;
import com.example.hotelbooking.payment.application.port.out.PaymentObservabilityContext;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGateway;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGatewayRegistry;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentId;
import com.example.hotelbooking.payment.domain.event.PaymentLifecycleEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovePaymentService {

  private static final String OUTCOME_APPROVED = "approved";
  private static final String OUTCOME_NOT_FOUND = "not_found";
  private static final String OUTCOME_FAILED = "failed";

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;
  private final PaymentStateChangePersistenceService persistenceService;
  private final PaymentObservabilityContext observabilityContext;
  private final PaymentMetrics paymentMetrics;

  public Payment approve(ApprovePaymentCommand command) {
    PaymentId paymentId = new PaymentId(command.paymentId());
    Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);

    if (optionalPayment.isEmpty()) {
      paymentMetrics.approvalProcessed(OUTCOME_NOT_FOUND);
      throw new PaymentNotFoundException(paymentId);
    }

    Payment payment = optionalPayment.get();

    try (PaymentObservabilityContext.ContextScope ignored =
        observabilityContext.openPayment(command.correlationId(), payment)) {
      String outcome = OUTCOME_FAILED;

      try {
        payment.markApproved();

        PaymentProviderGateway gateway = gatewayRegistry.getGateway(payment.getProvider());
        gateway.approve(payment.getProviderPaymentId());

        Payment savedPayment =
            persistenceService.save(
                payment, PaymentLifecycleEvent.approved(payment, command.correlationId(), null));

        outcome = OUTCOME_APPROVED;

        return savedPayment;
      } finally {
        paymentMetrics.approvalProcessed(outcome);
      }
    }
  }
}
