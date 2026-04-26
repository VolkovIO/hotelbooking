package com.example.hotelbooking.booking.application.port.out;

import java.util.UUID;

public record RoomTypeReference(UUID hotelId, UUID roomTypeId, int guestCapacity) {}
