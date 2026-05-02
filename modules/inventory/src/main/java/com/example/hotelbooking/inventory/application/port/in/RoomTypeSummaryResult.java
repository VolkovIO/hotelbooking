package com.example.hotelbooking.inventory.application.port.in;

import java.util.UUID;

public record RoomTypeSummaryResult(UUID roomTypeId, String name, int guestCapacity) {}
