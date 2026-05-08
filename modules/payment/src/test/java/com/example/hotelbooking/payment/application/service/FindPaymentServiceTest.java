package com.example.hotelbooking.payment.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentAmount;
import com.example.hotelbooking.payment.domain.PaymentCurrency;
import com.example.hotelbooking.payment.domain.PaymentId;
import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentStatus;
import com.example.hotelbooking.payment.domain.PaymentUserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindPaymentServiceTest {

  private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  @Mock private PaymentRepository paymentRepository;

  @Test
  void shouldFindPaymentById() {
    FindPaymentService service = new FindPaymentService(paymentRepository);
    Payment payment = payment();

    when(paymentRepository.findById(new PaymentId(PAYMENT_ID))).thenReturn(Optional.of(payment));

    assertSame(payment, service.findById(PAYMENT_ID));
  }

  @Test
  void shouldRejectUnknownPayment() {
    FindPaymentService service = new FindPaymentService(paymentRepository);

    when(paymentRepository.findById(new PaymentId(PAYMENT_ID))).thenReturn(Optional.empty());

    assertThrows(PaymentNotFoundException.class, () -> service.findById(PAYMENT_ID));
  }

  private Payment payment() {
    Instant now = Instant.now();

    return Payment.restore(
        new PaymentId(PAYMENT_ID),
        new BookingId(UUID.randomUUID()),
        new PaymentUserId(UUID.randomUUID()),
        new PaymentAmount(new BigDecimal("12500.00")),
        new PaymentCurrency("RUB"),
        PaymentProvider.FAKE,
        PaymentStatus.NEW,
        null,
        null,
        now,
        null,
        null,
        null,
        null,
        now);
  }
}
