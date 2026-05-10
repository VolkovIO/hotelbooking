package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.payment.PaymentResult;
import com.example.hotelbooking.booking.application.payment.PaymentStatus;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.PaymentClient;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStateException;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelPaymentSagaAction implements BookingSagaAction {

  private final BookingSagaRepository sagaRepository;
  private final PaymentClient paymentClient;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.CANCEL_PAYMENT;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    UUID paymentId = saga.getPaymentId();
    if (paymentId == null) {
      saga.markPaymentCancelled();
      return sagaRepository.save(saga);
    }

    PaymentResult payment = paymentClient.cancel(paymentId);

    if (payment.status() != PaymentStatus.CANCELLED) {
      throw new BookingSagaStateException(
          "Unexpected payment status during cancel payment: status=" + payment.status());
    }

    saga.markPaymentCancelled();
    sagaRepository.save(saga);

    log.info(
        "Payment cancelled by saga compensation: sagaId={}, bookingId={}, paymentId={}",
        saga.getId().value(),
        saga.getBookingId(),
        paymentId);

    return saga;
  }
}
