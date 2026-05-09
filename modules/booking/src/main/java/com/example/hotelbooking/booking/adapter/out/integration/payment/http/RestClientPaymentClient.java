package com.example.hotelbooking.booking.adapter.out.integration.payment.http;

import com.example.hotelbooking.booking.application.payment.PaymentAuthorizationRequest;
import com.example.hotelbooking.booking.application.payment.PaymentClientException;
import com.example.hotelbooking.booking.application.payment.PaymentResult;
import com.example.hotelbooking.booking.application.payment.PaymentStatus;
import com.example.hotelbooking.booking.application.port.out.PaymentClient;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
class RestClientPaymentClient implements PaymentClient {

  private final RestClient restClient;

  RestClientPaymentClient(
      RestClient.Builder restClientBuilder, PaymentHttpClientProperties properties) {
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
  }

  @Override
  public PaymentResult authorize(PaymentAuthorizationRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    PaymentAuthorizeHttpRequest httpRequest =
        new PaymentAuthorizeHttpRequest(
            request.bookingId(), request.userId().value(), request.amount(), request.currency());

    log.debug(
        "Authorizing payment: bookingId={}, userId={}, amount={}, currency={}",
        request.bookingId(),
        request.userId(),
        request.amount(),
        request.currency());

    return execute(
        "authorize payment",
        () ->
            restClient
                .post()
                .uri("/api/v1/payments/authorize")
                .body(httpRequest)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (httpRequest1, response) -> {
                      throw new PaymentClientException(
                          "Payment authorization failed with HTTP status "
                              + response.getStatusCode());
                    })
                .body(PaymentHttpResponse.class));
  }

  @Override
  public PaymentResult approve(UUID paymentId) {
    Objects.requireNonNull(paymentId, "paymentId must not be null");

    log.debug("Approving payment: paymentId={}", paymentId);

    return execute(
        "approve payment",
        () ->
            restClient
                .post()
                .uri("/api/v1/payments/{paymentId}/approve", paymentId)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (httpRequest, response) -> {
                      throw new PaymentClientException(
                          "Payment approval failed with HTTP status " + response.getStatusCode());
                    })
                .body(PaymentHttpResponse.class));
  }

  @Override
  public PaymentResult cancel(UUID paymentId) {
    Objects.requireNonNull(paymentId, "paymentId must not be null");

    log.debug("Cancelling payment: paymentId={}", paymentId);

    return execute(
        "cancel payment",
        () ->
            restClient
                .post()
                .uri("/api/v1/payments/{paymentId}/cancel", paymentId)
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    (httpRequest, response) -> {
                      throw new PaymentClientException(
                          "Payment cancellation failed with HTTP status "
                              + response.getStatusCode());
                    })
                .body(PaymentHttpResponse.class));
  }

  private PaymentResult execute(String operation, PaymentHttpCall call) {
    try {
      PaymentHttpResponse response = call.execute();

      if (response == null) {
        throw new PaymentClientException("Payment service returned empty response");
      }

      return toResult(response);
    } catch (PaymentClientException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new PaymentClientException("Failed to " + operation, exception);
    }
  }

  private PaymentResult toResult(PaymentHttpResponse response) {
    return new PaymentResult(
        response.paymentId(),
        response.bookingId(),
        response.userId(),
        response.amount(),
        response.currency(),
        PaymentStatus.valueOf(response.status()),
        response.provider(),
        response.providerPaymentId(),
        response.failureReason());
  }

  @FunctionalInterface
  private interface PaymentHttpCall {

    PaymentHttpResponse execute();
  }
}
