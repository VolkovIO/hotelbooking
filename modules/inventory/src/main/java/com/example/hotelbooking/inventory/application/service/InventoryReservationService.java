package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.port.in.ConfirmRoomHoldUseCase;
import com.example.hotelbooking.inventory.application.port.in.InventoryReservationUseCase;
import com.example.hotelbooking.inventory.application.port.in.ReleaseRoomHoldUseCase;
import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.application.port.out.RoomHoldRepository;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomHold;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservationService implements InventoryReservationUseCase {

  private final RoomAvailabilityRepository roomAvailabilityRepository;
  private final RoomHoldRepository roomHoldRepository;
  private final ReleaseRoomHoldUseCase releaseRoomHoldUseCase;
  private final ConfirmRoomHoldUseCase confirmRoomHoldUseCase;

  @Override
  public UUID placeHold(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    log.info(
        "Placing inventory hold: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms);

    LocalDate availabilityTo = checkOut.minusDays(1);

    if (availabilityTo.isBefore(checkIn)) {
      throw new InventoryDomainException("Invalid hold period");
    }

    long requiredDays = ChronoUnit.DAYS.between(checkIn, checkOut);

    List<RoomAvailability> availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            hotelId, roomTypeId, checkIn, availabilityTo);

    if (availabilityList.size() != requiredDays) {
      throw new InventoryDomainException("Availability is not configured for the full stay period");
    }

    List<RoomAvailability> updatedAvailability =
        availabilityList.stream().map(item -> item.placeHold(rooms)).toList();

    roomAvailabilityRepository.saveAll(updatedAvailability);

    RoomHold roomHold = RoomHold.create(hotelId, roomTypeId, checkIn, checkOut, rooms);
    roomHoldRepository.save(roomHold);

    log.info(
        "Inventory hold placed: holdId={}, hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        roomHold.getId(),
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms);

    return roomHold.getId();
  }

  @Override
  public void releaseHold(UUID holdId) {
    log.info("Releasing inventory hold: holdId={}", holdId);

    releaseRoomHoldUseCase.execute(holdId);

    log.info("Inventory hold released: holdId={}", holdId);
  }

  @Override
  public void confirmHold(UUID holdId) {
    log.info("Confirming inventory hold: holdId={}", holdId);

    confirmRoomHoldUseCase.execute(holdId);

    log.info("Inventory hold confirmed: holdId={}", holdId);
  }

  @Override
  public void cancelConfirmedReservation(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    log.info(
        "Cancelling confirmed inventory reservation: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms);

    LocalDate availabilityTo = checkOut.minusDays(1);

    if (availabilityTo.isBefore(checkIn)) {
      throw new InventoryDomainException("Invalid reservation cancellation period");
    }

    long requiredDays = ChronoUnit.DAYS.between(checkIn, checkOut);

    List<RoomAvailability> availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            hotelId, roomTypeId, checkIn, availabilityTo);

    if (availabilityList.size() != requiredDays) {
      throw new InventoryDomainException(
          "Availability is not configured for the full reservation cancellation period");
    }

    List<RoomAvailability> updatedAvailability =
        availabilityList.stream().map(item -> item.releaseBookedRooms(rooms)).toList();

    roomAvailabilityRepository.saveAll(updatedAvailability);

    log.info(
        "Confirmed inventory reservation cancelled: hotelId={}, roomTypeId={}, checkIn={}, checkOut={}, rooms={}",
        hotelId,
        roomTypeId,
        checkIn,
        checkOut,
        rooms);
  }
}
