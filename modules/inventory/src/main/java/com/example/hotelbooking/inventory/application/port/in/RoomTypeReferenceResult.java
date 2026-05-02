package com.example.hotelbooking.inventory.application.port.in;

import java.util.UUID;

public record RoomTypeReferenceResult(UUID hotelId, UUID roomTypeId, int guestCapacity) {}
