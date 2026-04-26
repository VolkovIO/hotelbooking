package com.example.hotelbooking.inventory.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

@Schema(description = "Request for setting room availability for a date range")
public record SetRoomAvailabilityRequest(
    @Schema(description = "Start date of the availability range", example = "2030-06-01") @NotNull LocalDate from,
    @Schema(description = "End date of the availability range", example = "2030-06-30") @NotNull LocalDate to,
    @Schema(description = "Total available rooms for each day in the range", example = "10")
        @Positive Integer totalRooms) {}
