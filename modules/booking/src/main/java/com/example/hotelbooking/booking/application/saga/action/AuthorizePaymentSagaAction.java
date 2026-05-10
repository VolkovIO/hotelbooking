package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.payment.PaymentAuthorizationRequest;
import com.example.hotelbooking.booking.application.payment.PaymentResult;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.PaymentClient;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaFailureReason;
import com.example.hotelbooking.booking.application.saga.BookingSagaStateException;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import com.example.hotelbooking.booking.domain.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizePaymentSagaAction implements BookingSagaAction {

  private final BookingSagaRepository sagaRepository;
  private final BookingSagaBookingLoader bookingLoader;
  private final PaymentClient paymentClient;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.AUTHORIZE_PAYMENT;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    Booking booking = bookingLoader.load(saga);

    PaymentAuthorizationRequest request =
        new PaymentAuthorizationRequest(
            saga.getBookingId(),
            booking.getUserId(),
            saga.getPaymentAmount(),
            saga.getPaymentCurrency());

    PaymentResult payment = paymentClient.authorize(request);

    if (payment.isAuthorized()) {
      saga.markPaymentAuthorized(payment.paymentId());
      sagaRepository.save(saga);

      log.info(
          "Payment authorized by saga: sagaId={}, bookingId={}, paymentId={}",
          saga.getId().value(),
          saga.getBookingId(),
          payment.paymentId());

      return saga;
    }

    if (payment.isDeclined()) {
      String reason =
          payment.failureReason() == null || payment.failureReason().isBlank()
              ? "payment was declined"
              : payment.failureReason();

      saga.markPaymentDeclined(new BookingSagaFailureReason(reason));
      sagaRepository.save(saga);

      log.info(
          "Payment declined by saga: sagaId={}, bookingId={}, paymentId={}, reason={}",
          saga.getId().value(),
          saga.getBookingId(),
          payment.paymentId(),
          reason);

      return saga;
    }

    throw new BookingSagaStateException(
        "Unexpected payment status during authorize payment: status=" + payment.status());
  }
}
