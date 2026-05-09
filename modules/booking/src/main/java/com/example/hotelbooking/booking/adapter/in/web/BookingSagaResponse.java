package com.example.hotelbooking.booking.adapter.in.web;

import com.example.hotelbooking.booking.application.saga.BookingSaga;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Booking saga orchestration response")
public record BookingSagaResponse(
    @Schema(
            description = "Unique saga identifier",
            example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        UUID sagaId,
    @Schema(
            description = "Unique booking identifier",
            example = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        UUID bookingId,
    @Schema(description = "Technical saga status", example = "COMPLETED") String sagaStatus,
    @Schema(description = "Current saga step", example = "COMPLETE") String currentStep,
    @Schema(
            description = "Payment identifier if payment was created",
            example = "cccccccc-cccc-cccc-cccc-cccccccccccc")
        UUID paymentId,
    @Schema(description = "Retry count for technical failures", example = "0") int retryCount,
    @Schema(description = "Last failure reason if saga failed or was compensated")
        String lastFailureReason) {

  public static BookingSagaResponse from(BookingSaga saga) {
    String failureReason = null;

    if (saga.getLastFailureReason() != null) {
      failureReason = saga.getLastFailureReason().value();
    }

    return new BookingSagaResponse(
        saga.getId().value(),
        saga.getBookingId(),
        saga.getStatus().name(),
        saga.getCurrentStep().name(),
        saga.getPaymentId(),
        saga.getRetryCount(),
        failureReason);
  }
}
