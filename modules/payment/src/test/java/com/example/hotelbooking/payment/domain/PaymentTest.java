package com.example.hotelbooking.payment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

  private static final PaymentCurrency CURRENCY = new PaymentCurrency("RUB");
  private static final PaymentAmount AMOUNT = new PaymentAmount(new BigDecimal("12500.00"));
  private static final PaymentProvider PAYMENT_PROVIDER = PaymentProvider.FAKE;
  private static final PaymentProviderPaymentId PROVIDER_PAYMENT_ID =
      new PaymentProviderPaymentId("fake-payment-123");
  private static final PaymentFailureReason FAILURE_REASON =
      new PaymentFailureReason("payment declined by fake provider");

  @Test
  void shouldCreateNewPayment() {
    Payment payment = newPayment();

    assertEquals(PaymentStatus.NEW, payment.getStatus());
    assertNotNull(payment.getId());
    assertNull(payment.getProviderPaymentId());
    assertNull(payment.getFailureReason());
    assertNull(payment.getAuthorizedAt());
    assertNull(payment.getApprovedAt());
    assertNull(payment.getDeclinedAt());
    assertNull(payment.getCancelledAt());
    assertTrue(payment.isNew());
    assertFalse(payment.isAuthorized());
    assertFalse(payment.isTerminal());
  }

  @Test
  void shouldAuthorizeNewPayment() {
    Payment payment = newPayment();

    payment.markAuthorized(PROVIDER_PAYMENT_ID);

    assertEquals(PaymentStatus.AUTHORIZED, payment.getStatus());
    assertEquals(PROVIDER_PAYMENT_ID, payment.getProviderPaymentId());
    assertNotNull(payment.getAuthorizedAt());
    assertTrue(payment.isAuthorized());
    assertFalse(payment.isTerminal());
  }

  @Test
  void shouldDeclineNewPayment() {
    Payment payment = newPayment();

    payment.markDeclined(FAILURE_REASON);

    assertEquals(PaymentStatus.DECLINED, payment.getStatus());
    assertEquals(FAILURE_REASON, payment.getFailureReason());
    assertNotNull(payment.getDeclinedAt());
    assertTrue(payment.isTerminal());
  }

  @Test
  void shouldApproveAuthorizedPayment() {
    Payment payment = authorizedPayment();

    payment.markApproved();

    assertEquals(PaymentStatus.APPROVED, payment.getStatus());
    assertEquals(PROVIDER_PAYMENT_ID, payment.getProviderPaymentId());
    assertNotNull(payment.getAuthorizedAt());
    assertNotNull(payment.getApprovedAt());
    assertTrue(payment.isTerminal());
  }

  @Test
  void shouldCancelAuthorizedPayment() {
    Payment payment = authorizedPayment();

    payment.markCancelled();

    assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
    assertEquals(PROVIDER_PAYMENT_ID, payment.getProviderPaymentId());
    assertNotNull(payment.getAuthorizedAt());
    assertNotNull(payment.getCancelledAt());
    assertTrue(payment.isTerminal());
  }

  @Test
  void shouldRejectApprovingNewPayment() {
    Payment payment = newPayment();

    assertThrows(PaymentDomainException.class, payment::markApproved);
  }

  @Test
  void shouldRejectCancellingNewPayment() {
    Payment payment = newPayment();

    assertThrows(PaymentDomainException.class, payment::markCancelled);
  }

  @Test
  void shouldRejectAuthorizingDeclinedPayment() {
    Payment payment = newPayment();

    payment.markDeclined(FAILURE_REASON);

    assertThrows(PaymentDomainException.class, () -> payment.markAuthorized(PROVIDER_PAYMENT_ID));
  }

  @Test
  void shouldRejectDecliningAuthorizedPayment() {
    Payment payment = authorizedPayment();

    assertThrows(PaymentDomainException.class, () -> payment.markDeclined(FAILURE_REASON));
  }

  @Test
  void shouldRejectAuthorizedPaymentWithoutProviderPaymentIdOnRestore() {
    Instant now = Instant.now();

    assertThrows(
        PaymentDomainException.class,
        () ->
            Payment.restore(
                PaymentId.newId(),
                new BookingId(UUID.randomUUID()),
                new PaymentUserId(UUID.randomUUID()),
                AMOUNT,
                CURRENCY,
                PAYMENT_PROVIDER,
                PaymentStatus.AUTHORIZED,
                null,
                null,
                now,
                now,
                null,
                null,
                null,
                now));
  }

  @Test
  void shouldRejectDeclinedPaymentWithoutFailureReasonOnRestore() {
    Instant now = Instant.now();

    assertThrows(
        PaymentDomainException.class,
        () ->
            Payment.restore(
                PaymentId.newId(),
                new BookingId(UUID.randomUUID()),
                new PaymentUserId(UUID.randomUUID()),
                AMOUNT,
                CURRENCY,
                PAYMENT_PROVIDER,
                PaymentStatus.DECLINED,
                null,
                null,
                now,
                null,
                null,
                now,
                null,
                now));
  }

  @Test
  void shouldNormalizeCurrencyToUpperCase() {
    PaymentCurrency currency = new PaymentCurrency("rub");

    assertEquals("RUB", currency.value());
  }

  @Test
  void shouldRejectNonPositiveAmount() {
    assertThrows(IllegalArgumentException.class, () -> new PaymentAmount(BigDecimal.ZERO));
  }

  private Payment newPayment() {
    return Payment.create(
        new BookingId(UUID.randomUUID()),
        new PaymentUserId(UUID.randomUUID()),
        AMOUNT,
        CURRENCY,
        PAYMENT_PROVIDER);
  }

  private Payment authorizedPayment() {
    Payment payment = newPayment();
    payment.markAuthorized(PROVIDER_PAYMENT_ID);
    return payment;
  }
}
