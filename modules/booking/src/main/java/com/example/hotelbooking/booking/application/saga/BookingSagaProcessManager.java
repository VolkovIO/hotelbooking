package com.example.hotelbooking.booking.application.saga;

import com.example.hotelbooking.booking.application.exception.BookingNotFoundException;
import com.example.hotelbooking.booking.application.payment.PaymentAuthorizationRequest;
import com.example.hotelbooking.booking.application.payment.PaymentResult;
import com.example.hotelbooking.booking.application.payment.PaymentStatus;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.port.out.PaymentClient;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import com.example.hotelbooking.booking.domain.BookingId;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simple orchestrated saga process manager for the booking flow.
 *
 * <p>This class is intentionally explicit and step-based. For a small educational project this is
 * easier to reason about than introducing a workflow engine immediately.
 *
 * <p>The process manager owns the distributed process, not the domain rules. Booking domain rules
 * still live inside the Booking aggregate. Payment and inventory are accessed through ports.
 *
 * <p>Important production note: external service calls are not wrapped into one distributed
 * transaction. Each service performs its own local transaction. If a later step fails, the saga
 * uses compensation steps.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.CyclomaticComplexity")
public class BookingSagaProcessManager {

  private static final int ROOMS_PER_BOOKING = 1;
  private static final int MAX_STEPS_PER_RUN = 20;

  private final BookingSagaRepository sagaRepository;
  private final BookingRepository bookingRepository;
  private final InventoryReservationPort inventoryReservationPort;
  private final PaymentClient paymentClient;

  /**
   * Continues saga execution from the currently persisted step.
   *
   * <p>For the first implementation we execute the saga synchronously until it reaches a terminal
   * state. Retry scheduling will be added as a separate step later.
   */
  public BookingSaga process(BookingSagaId sagaId) {
    Objects.requireNonNull(sagaId, "sagaId must not be null");

    BookingSaga saga =
        sagaRepository.findById(sagaId).orElseThrow(() -> new BookingSagaNotFoundException(sagaId));

    log.info(
        "Processing booking saga: sagaId={}, bookingId={}, status={}, currentStep={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getStatus(),
        saga.getCurrentStep());

    if (saga.isWaitingRetry()) {
      saga.resumeAfterRetry();
      sagaRepository.save(saga);
    }

    int executedSteps = 0;

    while (!saga.isFinished() && saga.getCurrentStep() != BookingSagaStep.COMPLETE) {
      if (executedSteps >= MAX_STEPS_PER_RUN) {
        throw new BookingSagaStateException(
            "Booking saga exceeded maximum steps per run: sagaId=" + saga.getId().value());
      }

      saga = executeNextStep(saga);
      executedSteps++;
    }

    log.info(
        "Booking saga processing finished: sagaId={}, bookingId={}, status={}, currentStep={}",
        saga.getId().value(),
        saga.getBookingId(),
        saga.getStatus(),
        saga.getCurrentStep());

    return saga;
  }

  private BookingSaga executeNextStep(BookingSaga saga) {
    return switch (saga.getCurrentStep()) {
      case HOLD_INVENTORY -> holdInventory(saga);
      case AUTHORIZE_PAYMENT -> authorizePayment(saga);
      case CONFIRM_BOOKING -> confirmBooking(saga);
      case APPROVE_PAYMENT -> approvePayment(saga);
      case CANCEL_PAYMENT -> cancelPayment(saga);
      case RELEASE_INVENTORY -> releaseInventory(saga);
      case CANCEL_BOOKING -> cancelBooking(saga);
      case COMPLETE -> saga;
    };
  }

  private BookingSaga holdInventory(BookingSaga saga) {
    Booking booking = loadBooking(saga);

    if (booking.isOnHold()) {
      log.debug(
          "Booking is already on hold, skipping inventory hold: bookingId={}",
          booking.getId().value());

      saga.markInventoryHeld();
      return sagaRepository.save(saga);
    }

    UUID holdId =
        inventoryReservationPort.placeHold(
            booking.getHotelId(),
            booking.getRoomTypeId(),
            booking.getStayPeriod().checkIn(),
            booking.getStayPeriod().checkOut(),
            ROOMS_PER_BOOKING);

    booking.placeOnHold(holdId);
    bookingRepository.save(booking);

    saga.markInventoryHeld();
    sagaRepository.save(saga);

    log.info(
        "Inventory hold placed by saga: sagaId={}, bookingId={}, holdId={}",
        saga.getId().value(),
        booking.getId().value(),
        holdId);

    return saga;
  }

  private BookingSaga authorizePayment(BookingSaga saga) {
    Booking booking = loadBooking(saga);

    PaymentAuthorizationRequest request =
        new PaymentAuthorizationRequest(
            saga.getBookingId(),
            booking.getUserId(),
            saga.getPaymentAmount(),
            saga.getPaymentCurrency());

    PaymentResult payment = paymentClient.authorize(request);

    if (payment.isAuthorized()) {
      saga.markPaymentAuthorized(payment.paymentId());
      sagaRepository.save(saga);

      log.info(
          "Payment authorized by saga: sagaId={}, bookingId={}, paymentId={}",
          saga.getId().value(),
          saga.getBookingId(),
          payment.paymentId());

      return saga;
    }

    if (payment.isDeclined()) {
      String reason =
          payment.failureReason() == null || payment.failureReason().isBlank()
              ? "payment was declined"
              : payment.failureReason();

      saga.markPaymentDeclined(new BookingSagaFailureReason(reason));
      sagaRepository.save(saga);

      log.info(
          "Payment declined by saga: sagaId={}, bookingId={}, paymentId={}, reason={}",
          saga.getId().value(),
          saga.getBookingId(),
          payment.paymentId(),
          reason);

      return saga;
    }

    throw unexpectedPaymentStatus("authorize payment", payment.status());
  }

  private BookingSaga confirmBooking(BookingSaga saga) {
    Booking booking = loadBooking(saga);

    if (booking.isConfirmed()) {
      log.debug(
          "Booking is already confirmed, skipping booking confirmation: bookingId={}",
          booking.getId().value());

      saga.markBookingConfirmed();
      return sagaRepository.save(saga);
    }

    UUID holdId = booking.getHoldId();
    if (holdId == null) {
      throw new BookingDomainException("Booking has no active hold to confirm");
    }

    inventoryReservationPort.confirmHold(holdId);
    booking.confirmHeldBooking();
    bookingRepository.save(booking);

    saga.markBookingConfirmed();
    sagaRepository.save(saga);

    log.info(
        "Booking confirmed by saga: sagaId={}, bookingId={}",
        saga.getId().value(),
        booking.getId().value());

    return saga;
  }

  private BookingSaga approvePayment(BookingSaga saga) {
    UUID paymentId = saga.getPaymentId();
    if (paymentId == null) {
      throw new BookingSagaStateException("Cannot approve payment before authorization");
    }

    PaymentResult payment = paymentClient.approve(paymentId);

    if (payment.status() != PaymentStatus.APPROVED) {
      throw unexpectedPaymentStatus("approve payment", payment.status());
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

  private BookingSaga cancelPayment(BookingSaga saga) {
    UUID paymentId = saga.getPaymentId();
    if (paymentId == null) {
      saga.markPaymentCancelled();
      return sagaRepository.save(saga);
    }

    PaymentResult payment = paymentClient.cancel(paymentId);

    if (payment.status() != PaymentStatus.CANCELLED) {
      throw unexpectedPaymentStatus("cancel payment", payment.status());
    }

    saga.markPaymentCancelled();
    sagaRepository.save(saga);

    log.info(
        "Payment cancelled by saga compensation: sagaId={}, bookingId={}, paymentId={}",
        saga.getId().value(),
        saga.getBookingId(),
        paymentId);

    return saga;
  }

  /**
   * Releases inventory as part of compensation.
   *
   * <p>In the current Booking domain model, releasing inventory and cancelling the booking are
   * closely connected. For an ON_HOLD booking, releasing the hold makes the booking CANCELLED. For
   * a CONFIRMED booking, we cancel the confirmed reservation and mark the booking CANCELLED.
   */
  private BookingSaga releaseInventory(BookingSaga saga) {
    Booking booking = loadBooking(saga);

    if (booking.isOnHold()) {
      UUID holdId = booking.getHoldId();
      if (holdId == null) {
        throw new BookingDomainException("Booking has no active hold to release");
      }

      inventoryReservationPort.releaseHold(holdId);
      booking.cancelHeldBooking();
      bookingRepository.save(booking);

      log.info(
          "Inventory hold released by saga compensation: sagaId={}, bookingId={}, holdId={}",
          saga.getId().value(),
          booking.getId().value(),
          holdId);
    } else if (booking.isConfirmed()) {
      inventoryReservationPort.cancelConfirmedReservation(
          booking.getHotelId(),
          booking.getRoomTypeId(),
          booking.getStayPeriod().checkIn(),
          booking.getStayPeriod().checkOut(),
          ROOMS_PER_BOOKING);

      booking.cancelConfirmedBooking();
      bookingRepository.save(booking);

      log.info(
          "Confirmed inventory reservation cancelled by saga compensation: sagaId={}, bookingId={}",
          saga.getId().value(),
          booking.getId().value());
    } else {
      log.debug(
          "Booking does not need inventory release: sagaId={}, bookingId={}, status={}",
          saga.getId().value(),
          booking.getId().value(),
          booking.getStatus());
    }

    saga.markInventoryReleased();
    sagaRepository.save(saga);

    return saga;
  }

  /**
   * Finalizes booking compensation.
   *
   * <p>Most of the actual booking cancellation happens in releaseInventory(), because the current
   * Booking aggregate couples inventory release with booking cancellation. This step exists to keep
   * the saga state machine explicit and to mark the compensation as completed.
   */
  private BookingSaga cancelBooking(BookingSaga saga) {
    saga.markBookingCancelled();
    sagaRepository.save(saga);

    log.info(
        "Booking saga compensation completed: sagaId={}, bookingId={}",
        saga.getId().value(),
        saga.getBookingId());

    return saga;
  }

  private Booking loadBooking(BookingSaga saga) {
    BookingId bookingId = new BookingId(saga.getBookingId());

    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new BookingNotFoundException(bookingId));
  }

  private BookingSagaStateException unexpectedPaymentStatus(
      String operation, PaymentStatus status) {
    return new BookingSagaStateException(
        "Unexpected payment status during " + operation + ": status=" + status);
  }
}
