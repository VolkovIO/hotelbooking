package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.AuthorizePaymentCommand;
import com.example.hotelbooking.payment.application.port.out.PaymentMetrics;
import com.example.hotelbooking.payment.application.port.out.PaymentObservabilityContext;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.application.provider.PaymentProviderAuthorizationRequest;
import com.example.hotelbooking.payment.application.provider.PaymentProviderAuthorizationResult;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGateway;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGatewayRegistry;
import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentAmount;
import com.example.hotelbooking.payment.domain.PaymentCurrency;
import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentUserId;
import com.example.hotelbooking.payment.domain.event.PaymentLifecycleEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizePaymentService {

  private static final String OUTCOME_AUTHORIZED = "authorized";
  private static final String OUTCOME_DECLINED = "declined";
  private static final String OUTCOME_EXISTING = "existing";
  private static final String OUTCOME_FAILED = "failed";

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;
  private final PaymentStateChangePersistenceService persistenceService;
  private final PaymentObservabilityContext observabilityContext;
  private final PaymentMetrics paymentMetrics;

  public Payment authorize(AuthorizePaymentCommand command) {
    BookingId bookingId = new BookingId(command.bookingId());

    try (PaymentObservabilityContext.ContextScope ignored =
        observabilityContext.openBooking(command.correlationId(), bookingId)) {
      Optional<Payment> existingPayment = paymentRepository.findByBookingId(bookingId);

      if (existingPayment.isPresent()) {
        paymentMetrics.authorizationProcessed(OUTCOME_EXISTING);
        return existingPayment.get();
      }

      return authorizeNewPayment(command, bookingId);
    }
  }

  private Payment authorizeNewPayment(AuthorizePaymentCommand command, BookingId bookingId) {
    Payment payment =
        Payment.create(
            bookingId,
            new PaymentUserId(command.userId()),
            new PaymentAmount(command.amount()),
            new PaymentCurrency(command.currency()),
            PaymentProvider.FAKE);

    try (PaymentObservabilityContext.ContextScope ignored =
        observabilityContext.openPayment(command.correlationId(), payment)) {
      String outcome = OUTCOME_FAILED;

      try {
        PaymentProviderGateway gateway = gatewayRegistry.getGateway(payment.getProvider());
        PaymentProviderAuthorizationResult result =
            gateway.authorize(
                new PaymentProviderAuthorizationRequest(
                    payment.getBookingId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    payment.getCurrency()));

        if (result.authorized()) {
          payment.markAuthorized(result.providerPaymentId());

          Payment savedPayment =
              persistenceService.save(
                  payment,
                  PaymentLifecycleEvent.authorized(payment, command.correlationId(), null));

          outcome = OUTCOME_AUTHORIZED;

          return savedPayment;
        }

        payment.markDeclined(result.failureReason());

        Payment savedPayment =
            persistenceService.save(
                payment, PaymentLifecycleEvent.declined(payment, command.correlationId(), null));

        outcome = OUTCOME_DECLINED;

        return savedPayment;
      } finally {
        paymentMetrics.authorizationProcessed(outcome);
      }
    }
  }
}
