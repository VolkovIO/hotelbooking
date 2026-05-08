package com.example.hotelbooking.payment.application.service;

import com.example.hotelbooking.payment.application.command.AuthorizePaymentCommand;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorizePaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentProviderGatewayRegistry gatewayRegistry;

  @Transactional
  public Payment authorize(AuthorizePaymentCommand command) {
    BookingId bookingId = new BookingId(command.bookingId());

    return paymentRepository
        .findByBookingId(bookingId) // Simple idempotency
        .orElseGet(() -> authorizeNewPayment(command, bookingId));
  }

  private Payment authorizeNewPayment(AuthorizePaymentCommand command, BookingId bookingId) {
    Payment payment =
        Payment.create(
            bookingId,
            new PaymentUserId(command.userId()),
            new PaymentAmount(command.amount()),
            new PaymentCurrency(command.currency()),
            PaymentProvider.FAKE);

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
    } else {
      payment.markDeclined(result.failureReason());
    }

    return paymentRepository.save(payment);
  }
}
