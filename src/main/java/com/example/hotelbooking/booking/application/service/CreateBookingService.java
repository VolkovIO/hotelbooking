package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.exception.GuestCountExceedsRoomCapacityException;
import com.example.hotelbooking.booking.application.exception.HotelReferenceNotFoundException;
import com.example.hotelbooking.booking.application.exception.RoomTypeReferenceNotFoundException;
import com.example.hotelbooking.booking.application.port.in.CreateBookingUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.StayPeriod;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBookingService implements CreateBookingUseCase {

  private final BookingRepository bookingRepository;
  private final InventoryLookupPort inventoryLookupPort;
  private final InventoryReservationPort inventoryReservationPort;

  @Override
  public Booking execute(CreateBookingCommand command) {
    if (!inventoryLookupPort.hotelExists(command.hotelId())) {
      throw new HotelReferenceNotFoundException(command.hotelId());
    }

    if (!inventoryLookupPort.roomTypeExists(command.hotelId(), command.roomTypeId())) {
      throw new RoomTypeReferenceNotFoundException(command.hotelId(), command.roomTypeId());
    }

    int guestCapacity =
        inventoryLookupPort
            .findRoomTypeGuestCapacity(command.hotelId(), command.roomTypeId())
            .orElseThrow(
                () ->
                    new RoomTypeReferenceNotFoundException(
                        command.hotelId(), command.roomTypeId()));

    if (command.guestCount() > guestCapacity) {
      throw new GuestCountExceedsRoomCapacityException(command.guestCount(), guestCapacity);
    }

    UUID holdId =
        inventoryReservationPort.placeHold(
            command.hotelId(), command.roomTypeId(), command.checkIn(), command.checkOut(), 1);

    StayPeriod stayPeriod = new StayPeriod(command.checkIn(), command.checkOut());

    Booking booking =
        Booking.create(command.hotelId(), command.roomTypeId(), stayPeriod, command.guestCount());

    booking.placeOnHold(holdId);

    return bookingRepository.save(booking);
  }
}
