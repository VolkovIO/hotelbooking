package com.example.hotelbooking.booking.api;

import com.example.hotelbooking.booking.domain.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Booking response")
public record BookingResponse(
    @Schema(
            description = "Unique booking identifier",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
        UUID bookingId,
    @Schema(description = "Hotel identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID hotelId,
    @Schema(description = "Room type identifier", example = "660e8400-e29b-41d4-a716-446655440000")
        UUID roomTypeId,
    @Schema(description = "Check-in date", example = "2026-05-10") LocalDate checkIn,
    @Schema(description = "Check-out date", example = "2026-05-15") LocalDate checkOut,
    @Schema(description = "Number of guests", example = "2") int guestCount,
    @Schema(description = "Current booking status", example = "ON_HOLD") String status) {

  public static BookingResponse from(Booking booking) {
    return new BookingResponse(
        booking.getId().value(),
        booking.getHotelId(),
        booking.getRoomTypeId(),
        booking.getStayPeriod().checkIn(),
        booking.getStayPeriod().checkOut(),
        booking.getGuestCount(),
        booking.getStatus().name());
  }
}
