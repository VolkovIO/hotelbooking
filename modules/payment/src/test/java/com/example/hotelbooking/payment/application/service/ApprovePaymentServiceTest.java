package com.example.hotelbooking.payment.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.payment.application.command.ApprovePaymentCommand;
import com.example.hotelbooking.payment.application.exception.PaymentNotFoundException;
import com.example.hotelbooking.payment.application.port.out.PaymentRepository;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGateway;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGatewayRegistry;
import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentAmount;
import com.example.hotelbooking.payment.domain.PaymentCurrency;
import com.example.hotelbooking.payment.domain.PaymentId;
import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentProviderPaymentId;
import com.example.hotelbooking.payment.domain.PaymentStatus;
import com.example.hotelbooking.payment.domain.PaymentUserId;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovePaymentServiceTest {

  private static final UUID PAYMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final PaymentProviderPaymentId PROVIDER_PAYMENT_ID =
      new PaymentProviderPaymentId("fake-payment-123");

  @Mock private PaymentRepository paymentRepository;

  @Mock private PaymentProviderGatewayRegistry gatewayRegistry;

  @Mock private PaymentProviderGateway gateway;

  @Test
  void shouldApproveAuthorizedPayment() {
    ApprovePaymentService service = newService();
    Payment payment = authorizedPayment();

    when(paymentRepository.findById(new PaymentId(PAYMENT_ID))).thenReturn(Optional.of(payment));
    when(gatewayRegistry.getGateway(PaymentProvider.FAKE)).thenReturn(gateway);
    when(paymentRepository.save(payment)).thenReturn(payment);

    Payment result = service.approve(new ApprovePaymentCommand(PAYMENT_ID));

    assertEquals(PaymentStatus.APPROVED, result.getStatus());
    verify(gateway).approve(PROVIDER_PAYMENT_ID);
    verify(paymentRepository).save(payment);
  }

  @Test
  void shouldRejectUnknownPayment() {
    ApprovePaymentService service = newService();

    when(paymentRepository.findById(new PaymentId(PAYMENT_ID))).thenReturn(Optional.empty());

    assertThrows(
        PaymentNotFoundException.class,
        () -> service.approve(new ApprovePaymentCommand(PAYMENT_ID)));
  }

  private ApprovePaymentService newService() {
    return new ApprovePaymentService(paymentRepository, gatewayRegistry);
  }

  private Payment authorizedPayment() {
    Payment payment =
        Payment.restore(
            new PaymentId(PAYMENT_ID),
            new BookingId(UUID.randomUUID()),
            new PaymentUserId(UUID.randomUUID()),
            new PaymentAmount(new BigDecimal("12500.00")),
            new PaymentCurrency("RUB"),
            PaymentProvider.FAKE,
            PaymentStatus.NEW,
            null,
            null,
            java.time.Instant.now(),
            null,
            null,
            null,
            null,
            java.time.Instant.now());

    payment.markAuthorized(PROVIDER_PAYMENT_ID);
    return payment;
  }
}
