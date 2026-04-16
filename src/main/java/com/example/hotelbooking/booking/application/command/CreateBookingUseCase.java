package com.example.hotelbooking.booking.application.command;

import com.example.hotelbooking.booking.application.exception.HotelReferenceNotFoundException;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.exception.RoomTypeReferenceNotFoundException;
import com.example.hotelbooking.booking.application.port.BookingRepository;
import com.example.hotelbooking.booking.application.port.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.InventoryReservationPort;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBookingUseCase {

  private final BookingRepository bookingRepository;
  private final InventoryLookupPort inventoryLookupPort;
  private final InventoryReservationPort inventoryReservationPort;

  public Booking execute(CreateBookingCommand command) {
    if (!inventoryLookupPort.hotelExists(command.hotelId())) {
      throw new HotelReferenceNotFoundException(command.hotelId());
    }

    if (!inventoryLookupPort.roomTypeExists(command.hotelId(), command.roomTypeId())) {
      throw new RoomTypeReferenceNotFoundException(command.hotelId(), command.roomTypeId());
    }

    UUID holdId;
    try {
      holdId =
          inventoryReservationPort.placeHold(
              command.hotelId(), command.roomTypeId(), command.checkIn(), command.checkOut(), 1);
    } catch (InventoryDomainException exception) {
      throw new RoomHoldFailedException(
          "Failed to place room hold for hotel %s and room type %s"
              .formatted(command.hotelId(), command.roomTypeId()),
          exception);
    }

    StayPeriod stayPeriod = new StayPeriod(command.checkIn(), command.checkOut());

    Booking booking =
        Booking.create(command.hotelId(), command.roomTypeId(), stayPeriod, command.guestCount());

    booking.placeOnHold(holdId);

    return bookingRepository.save(booking);
  }
}
