package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.exception.GuestCountExceedsRoomCapacityException;
import com.example.hotelbooking.booking.application.exception.RoomTypeReferenceNotFoundException;
import com.example.hotelbooking.booking.application.port.in.CreateBookingUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.port.out.RoomTypeReference;
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
    final StayPeriod stayPeriod = new StayPeriod(command.checkIn(), command.checkOut());

    RoomTypeReference roomTypeReference =
        inventoryLookupPort
            .findRoomTypeReference(command.hotelId(), command.roomTypeId())
            .orElseThrow(
                () ->
                    new RoomTypeReferenceNotFoundException(
                        command.hotelId(), command.roomTypeId()));

    if (command.guestCount() > roomTypeReference.guestCapacity()) {
      throw new GuestCountExceedsRoomCapacityException(
          command.guestCount(), roomTypeReference.guestCapacity());
    }

    UUID holdId =
        inventoryReservationPort.placeHold(
            command.hotelId(), command.roomTypeId(), command.checkIn(), command.checkOut(), 1);

    Booking booking =
        Booking.create(command.hotelId(), command.roomTypeId(), stayPeriod, command.guestCount());

    booking.placeOnHold(holdId);

    return bookingRepository.save(booking);
  }
}
