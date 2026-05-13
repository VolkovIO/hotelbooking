package com.example.hotelbooking.payment.adapter.in.web;

import com.example.hotelbooking.payment.application.command.ApprovePaymentCommand;
import com.example.hotelbooking.payment.application.command.AuthorizePaymentCommand;
import com.example.hotelbooking.payment.application.command.CancelPaymentCommand;
import com.example.hotelbooking.payment.application.service.ApprovePaymentService;
import com.example.hotelbooking.payment.application.service.AuthorizePaymentService;
import com.example.hotelbooking.payment.application.service.CancelPaymentService;
import com.example.hotelbooking.payment.application.service.FindPaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
class PaymentController {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

  private final AuthorizePaymentService authorizePaymentService;
  private final ApprovePaymentService approvePaymentService;
  private final CancelPaymentService cancelPaymentService;
  private final FindPaymentService findPaymentService;

  @PostMapping("/authorize")
  PaymentResponse authorize(
      @Valid @RequestBody AuthorizePaymentRequest request,
      @RequestHeader(name = CORRELATION_ID_HEADER, required = false) UUID correlationId) {
    return PaymentResponse.from(
        authorizePaymentService.authorize(
            new AuthorizePaymentCommand(
                request.bookingId(),
                request.userId(),
                request.amount(),
                request.currency(),
                effectiveCorrelationId(correlationId))));
  }

  @PostMapping("/{paymentId}/approve")
  PaymentResponse approve(
      @PathVariable UUID paymentId,
      @RequestHeader(name = CORRELATION_ID_HEADER, required = false) UUID correlationId) {
    return PaymentResponse.from(
        approvePaymentService.approve(
            new ApprovePaymentCommand(paymentId, effectiveCorrelationId(correlationId))));
  }

  @PostMapping("/{paymentId}/cancel")
  PaymentResponse cancel(
      @PathVariable UUID paymentId,
      @RequestHeader(name = CORRELATION_ID_HEADER, required = false) UUID correlationId) {
    return PaymentResponse.from(
        cancelPaymentService.cancel(
            new CancelPaymentCommand(paymentId, effectiveCorrelationId(correlationId))));
  }

  @GetMapping("/{paymentId}")
  PaymentResponse findById(@PathVariable UUID paymentId) {
    return PaymentResponse.from(findPaymentService.findById(paymentId));
  }

  private UUID effectiveCorrelationId(UUID correlationId) {
    if (correlationId != null) {
      return correlationId;
    }

    return UUID.randomUUID();
  }
}
