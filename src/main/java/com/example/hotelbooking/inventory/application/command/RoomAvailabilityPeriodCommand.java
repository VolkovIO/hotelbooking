package com.example.hotelbooking.inventory.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record RoomAvailabilityPeriodCommand(
    UUID hotelId, UUID roomTypeId, LocalDate startDate, LocalDate endDate, int totalRooms) {}
