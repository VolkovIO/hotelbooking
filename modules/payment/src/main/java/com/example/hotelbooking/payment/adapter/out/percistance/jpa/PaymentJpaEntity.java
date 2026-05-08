package com.example.hotelbooking.payment.adapter.out.percistance.jpa;

import com.example.hotelbooking.payment.domain.BookingId;
import com.example.hotelbooking.payment.domain.Payment;
import com.example.hotelbooking.payment.domain.PaymentAmount;
import com.example.hotelbooking.payment.domain.PaymentCurrency;
import com.example.hotelbooking.payment.domain.PaymentFailureReason;
import com.example.hotelbooking.payment.domain.PaymentId;
import com.example.hotelbooking.payment.domain.PaymentProvider;
import com.example.hotelbooking.payment.domain.PaymentProviderPaymentId;
import com.example.hotelbooking.payment.domain.PaymentStatus;
import com.example.hotelbooking.payment.domain.PaymentUserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class PaymentJpaEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "booking_id", nullable = false)
  private UUID bookingId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "provider", nullable = false, length = 32)
  private String provider;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "provider_payment_id", length = 128)
  private String providerPaymentId;

  @Column(name = "failure_reason", length = 1000)
  private String failureReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "authorized_at")
  private Instant authorizedAt;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "declined_at")
  private Instant declinedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  static PaymentJpaEntity from(Payment payment) {
    return new PaymentJpaEntity(
        payment.getId().value(),
        payment.getBookingId().value(),
        payment.getUserId().value(),
        payment.getAmount().value(),
        payment.getCurrency().value(),
        payment.getProvider().name(),
        payment.getStatus().name(),
        providerPaymentIdValue(payment),
        failureReasonValue(payment),
        payment.getCreatedAt(),
        payment.getAuthorizedAt(),
        payment.getApprovedAt(),
        payment.getDeclinedAt(),
        payment.getCancelledAt(),
        payment.getUpdatedAt());
  }

  Payment toDomain() {
    return Payment.restore(
        new PaymentId(id),
        new BookingId(bookingId),
        new PaymentUserId(userId),
        new PaymentAmount(amount),
        new PaymentCurrency(currency),
        PaymentProvider.valueOf(provider),
        PaymentStatus.valueOf(status),
        nullableProviderPaymentId(providerPaymentId),
        nullableFailureReason(failureReason),
        createdAt,
        authorizedAt,
        approvedAt,
        declinedAt,
        cancelledAt,
        updatedAt);
  }

  private static String providerPaymentIdValue(Payment payment) {
    if (payment.getProviderPaymentId() == null) {
      return null;
    }

    return payment.getProviderPaymentId().value();
  }

  private static String failureReasonValue(Payment payment) {
    if (payment.getFailureReason() == null) {
      return null;
    }

    return payment.getFailureReason().value();
  }

  private PaymentProviderPaymentId nullableProviderPaymentId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return new PaymentProviderPaymentId(value);
  }

  private PaymentFailureReason nullableFailureReason(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return new PaymentFailureReason(value);
  }
}
