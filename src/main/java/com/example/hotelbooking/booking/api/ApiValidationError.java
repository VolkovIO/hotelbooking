package com.example.hotelbooking.booking.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Validation error details")
public record ApiValidationError(
    @Schema(description = "Field name", example = "guestCount") String field,
    @Schema(description = "Validation error message", example = "must be greater than 0")
        String message) {}
