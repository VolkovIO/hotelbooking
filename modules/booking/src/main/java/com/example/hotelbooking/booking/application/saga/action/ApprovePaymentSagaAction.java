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
public class ApprovePaymentSagaAction implements BookingSagaAction {

  private final BookingSagaRepository sagaRepository;
  private final PaymentClient paymentClient;

  @Override
  public BookingSagaStep step() {
    return BookingSagaStep.APPROVE_PAYMENT;
  }

  @Override
  public BookingSaga execute(BookingSaga saga) {
    UUID paymentId = saga.getPaymentId();
    if (paymentId == null) {
      throw new BookingSagaStateException("Cannot approve payment before authorization");
    }

    UUID correlationId = saga.getId().value();

    PaymentResult payment = paymentClient.approve(paymentId, correlationId);

    if (payment.status() != PaymentStatus.APPROVED) {
      throw new BookingSagaStateException(
          "Unexpected payment status during approve payment: status=" + payment.status());
    }

    saga.markPaymentApproved();
    sagaRepository.save(saga);

    log.info(
        "Payment approved by saga: sagaId={}, bookingId={}, paymentId={}",
        saga.getId().value(),
        saga.getBookingId(),
        paymentId);

    return saga;
  }
}
