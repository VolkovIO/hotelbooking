package com.example.hotelbooking.inventory.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request for adding a room type to a hotel")
public record AddRoomTypeRequest(
    @Schema(description = "Room type name", example = "Standard") @NotBlank String name,
    @Schema(description = "Maximum guest capacity", example = "2") @Positive int guestCapacity) {}
