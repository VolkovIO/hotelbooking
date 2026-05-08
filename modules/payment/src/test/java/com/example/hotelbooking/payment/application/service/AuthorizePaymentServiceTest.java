package com.example.hotelbooking.payment.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.example.hotelbooking.payment.domain.PaymentFailureReason;
import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentProviderPaymentId;
import com.example.hotelbooking.payment.domain.PaymentStatus;
import com.example.hotelbooking.payment.domain.PaymentUserId;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizePaymentServiceTest {

  private static final UUID BOOKING_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final BigDecimal AMOUNT = new BigDecimal("12500.00");
  private static final String CURRENCY = "RUB";
  private static final PaymentProviderPaymentId PROVIDER_PAYMENT_ID =
      new PaymentProviderPaymentId("fake-payment-123");
  private static final PaymentFailureReason FAILURE_REASON =
      new PaymentFailureReason("payment declined");

  @Mock private PaymentRepository paymentRepository;

  @Mock private PaymentProviderGatewayRegistry gatewayRegistry;

  @Mock private PaymentProviderGateway gateway;

  @Test
  void shouldAuthorizeNewPayment() {
    AuthorizePaymentService service = newService();
    AuthorizePaymentCommand command = command();

    when(paymentRepository.findByBookingId(new BookingId(BOOKING_ID))).thenReturn(Optional.empty());
    when(gatewayRegistry.getGateway(PaymentProvider.FAKE)).thenReturn(gateway);
    when(gateway.authorize(any(PaymentProviderAuthorizationRequest.class)))
        .thenReturn(PaymentProviderAuthorizationResult.authorized(PROVIDER_PAYMENT_ID));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Payment result = service.authorize(command);

    assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
    assertEquals(PROVIDER_PAYMENT_ID, result.getProviderPaymentId());

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());
    assertEquals(PaymentStatus.AUTHORIZED, paymentCaptor.getValue().getStatus());
  }

  @Test
  void shouldDeclineNewPayment() {
    AuthorizePaymentService service = newService();
    AuthorizePaymentCommand command = command();

    when(paymentRepository.findByBookingId(new BookingId(BOOKING_ID))).thenReturn(Optional.empty());
    when(gatewayRegistry.getGateway(PaymentProvider.FAKE)).thenReturn(gateway);
    when(gateway.authorize(any(PaymentProviderAuthorizationRequest.class)))
        .thenReturn(PaymentProviderAuthorizationResult.declined(FAILURE_REASON));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Payment result = service.authorize(command);

    assertEquals(PaymentStatus.DECLINED, result.getStatus());
    assertEquals(FAILURE_REASON, result.getFailureReason());
  }

  @Test
  void shouldReturnExistingPaymentForSameBooking() {
    AuthorizePaymentService service = newService();
    Payment existingPayment = existingAuthorizedPayment();

    when(paymentRepository.findByBookingId(new BookingId(BOOKING_ID)))
        .thenReturn(Optional.of(existingPayment));

    Payment result = service.authorize(command());

    assertSame(existingPayment, result);
    verify(gatewayRegistry, never()).getGateway(PaymentProvider.FAKE);
    verify(paymentRepository, never()).save(any(Payment.class));
  }

  private AuthorizePaymentService newService() {
    return new AuthorizePaymentService(paymentRepository, gatewayRegistry);
  }

  private AuthorizePaymentCommand command() {
    return new AuthorizePaymentCommand(BOOKING_ID, USER_ID, AMOUNT, CURRENCY);
  }

  private Payment existingAuthorizedPayment() {
    Payment payment =
        Payment.create(
            new BookingId(BOOKING_ID),
            new PaymentUserId(USER_ID),
            new PaymentAmount(AMOUNT),
            new PaymentCurrency(CURRENCY),
            PaymentProvider.FAKE);

    payment.markAuthorized(PROVIDER_PAYMENT_ID);
    return payment;
  }
}
