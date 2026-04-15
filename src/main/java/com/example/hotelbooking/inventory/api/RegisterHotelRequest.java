package com.example.hotelbooking.inventory.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for registering a hotel")
public record RegisterHotelRequest(
    @Schema(description = "Hotel name", example = "Riviera Hotel") @NotBlank String name,
    @Schema(description = "City where the hotel is located", example = "Kazan") @NotBlank String city) {}
