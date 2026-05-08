package com.example.hotelbooking.payment.adapter.out.provider.fake;

import com.example.hotelbooking.payment.application.provider.PaymentProviderAuthorizationRequest;
import com.example.hotelbooking.payment.application.provider.PaymentProviderAuthorizationResult;
import com.example.hotelbooking.payment.application.provider.PaymentProviderGateway;
import com.example.hotelbooking.payment.domain.PaymentFailureReason;
import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentProviderPaymentId;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.payment.provider.fake",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
class FakePaymentProviderGateway implements PaymentProviderGateway {

  private static final String PROVIDER_PAYMENT_ID_PREFIX = "fake-payment-";
  private static final String ALWAYS_DECLINED_REASON = "payment declined by fake provider";
  private static final String AMOUNT_DECLINED_REASON =
      "payment amount exceeds fake provider decline threshold";

  private final FakePaymentProviderProperties properties;

  @Override
  public PaymentProvider provider() {
    return PaymentProvider.FAKE;
  }

  @Override
  public PaymentProviderAuthorizationResult authorize(PaymentProviderAuthorizationRequest request) {
    if (shouldDecline(request)) {
      log.info(
          "Fake payment provider declined payment authorization: bookingId={}, userId={}, amount={}, currency={}",
          request.bookingId().value(),
          request.userId().value(),
          request.amount().value(),
          request.currency().value());

      return PaymentProviderAuthorizationResult.declined(declineReason(request));
    }

    PaymentProviderPaymentId providerPaymentId =
        new PaymentProviderPaymentId(PROVIDER_PAYMENT_ID_PREFIX + UUID.randomUUID());

    log.info(
        "Fake payment provider authorized payment: "
            + "bookingId={}, userId={}, amount={}, currency={}, providerPaymentId={}",
        request.bookingId().value(),
        request.userId().value(),
        request.amount().value(),
        request.currency().value(),
        providerPaymentId.value());

    return PaymentProviderAuthorizationResult.authorized(providerPaymentId);
  }

  @Override
  public void approve(PaymentProviderPaymentId providerPaymentId) {
    log.info(
        "Fake payment provider approved payment: providerPaymentId={}", providerPaymentId.value());
  }

  @Override
  public void cancel(PaymentProviderPaymentId providerPaymentId) {
    log.info(
        "Fake payment provider cancelled payment: providerPaymentId={}", providerPaymentId.value());
  }

  private boolean shouldDecline(PaymentProviderAuthorizationRequest request) {
    return properties.isAlwaysDecline() || amountExceedsDeclineThreshold(request);
  }

  private boolean amountExceedsDeclineThreshold(PaymentProviderAuthorizationRequest request) {
    BigDecimal threshold = properties.getDeclineAmountGreaterThan();

    if (threshold == null) {
      return false;
    }

    return request.amount().value().compareTo(threshold) > 0;
  }

  private PaymentFailureReason declineReason(PaymentProviderAuthorizationRequest request) {
    if (amountExceedsDeclineThreshold(request)) {
      return new PaymentFailureReason(AMOUNT_DECLINED_REASON);
    }

    return new PaymentFailureReason(ALWAYS_DECLINED_REASON);
  }
}
