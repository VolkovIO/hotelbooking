package com.example.hotelbooking.payment.domain;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * Payment represents an authorization-based payment lifecycle for a booking.
 *
 * <p>The aggregate protects valid payment status transitions. Real provider communication is
 * performed outside of the domain through payment provider ports.
 */
@Getter
@SuppressWarnings("PMD.CyclomaticComplexity")
public final class Payment {

  private final PaymentId id;
  private final BookingId bookingId;
  private final PaymentUserId userId;
  private final PaymentAmount amount;
  private final PaymentCurrency currency;
  private final PaymentProvider provider;
  private PaymentStatus status;
  private PaymentProviderPaymentId providerPaymentId;
  private PaymentFailureReason failureReason;
  private final Instant createdAt;
  private Instant authorizedAt;
  private Instant approvedAt;
  private Instant declinedAt;
  private Instant cancelledAt;
  private Instant updatedAt;

  private Payment(
      PaymentId id,
      BookingId bookingId,
      PaymentUserId userId,
      PaymentAmount amount,
      PaymentCurrency currency,
      PaymentProvider provider,
      PaymentStatus status,
      PaymentProviderPaymentId providerPaymentId,
      PaymentFailureReason failureReason,
      Instant createdAt,
      Instant authorizedAt,
      Instant approvedAt,
      Instant declinedAt,
      Instant cancelledAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.bookingId = Objects.requireNonNull(bookingId, "bookingId must not be null");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.amount = Objects.requireNonNull(amount, "amount must not be null");
    this.currency = Objects.requireNonNull(currency, "currency must not be null");
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.providerPaymentId = providerPaymentId;
    this.failureReason = failureReason;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.authorizedAt = authorizedAt;
    this.approvedAt = approvedAt;
    this.declinedAt = declinedAt;
    this.cancelledAt = cancelledAt;
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

    validateState();
  }

  public static Payment create(
      BookingId bookingId,
      PaymentUserId userId,
      PaymentAmount amount,
      PaymentCurrency currency,
      PaymentProvider provider) {
    Instant now = Instant.now();

    return new Payment(
        PaymentId.newId(),
        bookingId,
        userId,
        amount,
        currency,
        provider,
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

  public static Payment restore(
      PaymentId id,
      BookingId bookingId,
      PaymentUserId userId,
      PaymentAmount amount,
      PaymentCurrency currency,
      PaymentProvider provider,
      PaymentStatus status,
      PaymentProviderPaymentId providerPaymentId,
      PaymentFailureReason failureReason,
      Instant createdAt,
      Instant authorizedAt,
      Instant approvedAt,
      Instant declinedAt,
      Instant cancelledAt,
      Instant updatedAt) {
    return new Payment(
        id,
        bookingId,
        userId,
        amount,
        currency,
        provider,
        status,
        providerPaymentId,
        failureReason,
        createdAt,
        authorizedAt,
        approvedAt,
        declinedAt,
        cancelledAt,
        updatedAt);
  }

  public void markAuthorized(PaymentProviderPaymentId providerPaymentId) {
    requireStatus(PaymentStatus.NEW, "authorize");

    this.status = PaymentStatus.AUTHORIZED;
    this.providerPaymentId =
        Objects.requireNonNull(providerPaymentId, "providerPaymentId must not be null");
    this.authorizedAt = Instant.now();
    this.updatedAt = Instant.now();

    validateState();
  }

  public void markDeclined(PaymentFailureReason reason) {
    requireStatus(PaymentStatus.NEW, "decline");

    this.status = PaymentStatus.DECLINED;
    this.failureReason = Objects.requireNonNull(reason, "reason must not be null");
    this.declinedAt = Instant.now();
    this.updatedAt = Instant.now();

    validateState();
  }

  public void markApproved() {
    requireStatus(PaymentStatus.AUTHORIZED, "approve");

    this.status = PaymentStatus.APPROVED;
    this.approvedAt = Instant.now();
    this.updatedAt = Instant.now();

    validateState();
  }

  public void markCancelled() {
    requireStatus(PaymentStatus.AUTHORIZED, "cancel");

    this.status = PaymentStatus.CANCELLED;
    this.cancelledAt = Instant.now();
    this.updatedAt = Instant.now();

    validateState();
  }

  public boolean isNew() {
    return status == PaymentStatus.NEW;
  }

  public boolean isAuthorized() {
    return status == PaymentStatus.AUTHORIZED;
  }

  public boolean isTerminal() {
    return status == PaymentStatus.DECLINED
        || status == PaymentStatus.APPROVED
        || status == PaymentStatus.CANCELLED;
  }

  private void requireStatus(PaymentStatus expectedStatus, String operation) {
    if (status != expectedStatus) {
      throw new PaymentDomainException("Cannot " + operation + " payment with status " + status);
    }
  }

  private void validateState() {
    validateAuthorizedState();
    validateDeclinedState();
    validateApprovedState();
    validateCancelledState();
  }

  private void validateAuthorizedState() {
    if (status == PaymentStatus.AUTHORIZED && providerPaymentId == null) {
      throw new PaymentDomainException("authorized payment must have provider payment id");
    }

    if (status != PaymentStatus.AUTHORIZED
        && status != PaymentStatus.APPROVED
        && status != PaymentStatus.CANCELLED
        && providerPaymentId != null) {
      throw new PaymentDomainException("only authorized payment flow can have provider payment id");
    }
  }

  private void validateDeclinedState() {
    if (status == PaymentStatus.DECLINED && failureReason == null) {
      throw new PaymentDomainException("declined payment must have failure reason");
    }

    if (status != PaymentStatus.DECLINED && failureReason != null) {
      throw new PaymentDomainException("only declined payment can have failure reason");
    }

    if (status == PaymentStatus.DECLINED && declinedAt == null) {
      throw new PaymentDomainException("declined payment must have declinedAt");
    }
  }

  private void validateApprovedState() {
    if (status == PaymentStatus.APPROVED && providerPaymentId == null) {
      throw new PaymentDomainException("approved payment must have provider payment id");
    }

    if (status == PaymentStatus.APPROVED && authorizedAt == null) {
      throw new PaymentDomainException("approved payment must have authorizedAt");
    }

    if (status == PaymentStatus.APPROVED && approvedAt == null) {
      throw new PaymentDomainException("approved payment must have approvedAt");
    }
  }

  private void validateCancelledState() {
    if (status == PaymentStatus.CANCELLED && providerPaymentId == null) {
      throw new PaymentDomainException("cancelled payment must have provider payment id");
    }

    if (status == PaymentStatus.CANCELLED && authorizedAt == null) {
      throw new PaymentDomainException("cancelled payment must have authorizedAt");
    }

    if (status == PaymentStatus.CANCELLED && cancelledAt == null) {
      throw new PaymentDomainException("cancelled payment must have cancelledAt");
    }
  }
}
