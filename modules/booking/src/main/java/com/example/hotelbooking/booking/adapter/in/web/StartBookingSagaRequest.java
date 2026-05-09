package com.example.hotelbooking.booking.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request for starting booking saga orchestration")
public record StartBookingSagaRequest(
    @Schema(
            description = "Unique identifier of the hotel selected for booking",
            example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID hotelId,
    @Schema(
            description = "Unique identifier of the room type selected for booking",
            example = "660e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID roomTypeId,
    @Schema(description = "Check-in date", example = "2026-06-10") @NotNull @Future LocalDate checkIn,
    @Schema(description = "Check-out date", example = "2026-06-15") @NotNull @Future LocalDate checkOut,
    @Schema(description = "Number of guests staying in the room", example = "2") @Positive int guestCount,
    @Schema(description = "Payment amount to authorize", example = "12500.00")
        @NotNull @DecimalMin(value = "0.01") BigDecimal paymentAmount,
    @Schema(description = "Payment currency code", example = "RUB")
        @NotBlank @Size(min = 3, max = 3) String paymentCurrency) {}
