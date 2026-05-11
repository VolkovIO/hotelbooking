package com.example.hotelbooking.inventory.application.service;

import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.domain.InventoryDomainException;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AtomicRoomHoldReservationService {

  private final RoomAvailabilityRepository roomAvailabilityRepository;

  void reserve(UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
    List<LocalDate> stayDates = stayDates(checkIn, checkOut);

    validateAvailabilityConfigured(hotelId, roomTypeId, checkIn, checkOut, stayDates.size());

    reserveHoldAtomically(hotelId, roomTypeId, stayDates, rooms);
  }

  private List<LocalDate> stayDates(LocalDate checkIn, LocalDate checkOut) {
    return checkIn.datesUntil(checkOut).toList();
  }

  private void validateAvailabilityConfigured(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int requiredDays) {
    List<RoomAvailability> availabilityList =
        roomAvailabilityRepository.findByRoomTypeAndDateRange(
            hotelId, roomTypeId, checkIn, checkOut.minusDays(1));

    if (availabilityList.size() != requiredDays) {
      throw new InventoryDomainException("Availability is not configured for the full stay period");
    }
  }

  private void reserveHoldAtomically(
      UUID hotelId, UUID roomTypeId, List<LocalDate> stayDates, int rooms) {
    List<LocalDate> reservedDates = new ArrayList<>();

    for (LocalDate date : stayDates) {
      boolean reserved = roomAvailabilityRepository.tryPlaceHold(hotelId, roomTypeId, date, rooms);

      if (!reserved) {
        rollbackReservedDates(hotelId, roomTypeId, reservedDates, rooms);
        throw new InventoryDomainException("Not enough rooms available to place hold");
      }

      reservedDates.add(date);
    }
  }

  private void rollbackReservedDates(
      UUID hotelId, UUID roomTypeId, List<LocalDate> reservedDates, int rooms) {
    for (int index = reservedDates.size() - 1; index >= 0; index--) {
      LocalDate date = reservedDates.get(index);

      boolean released = roomAvailabilityRepository.releaseHold(hotelId, roomTypeId, date, rooms);

      if (!released) {
        log.warn(
            "Failed to rollback inventory hold reservation: hotelId={}, roomTypeId={}, date={}, rooms={}",
            hotelId,
            roomTypeId,
            date,
            rooms);
      }
    }
  }
}
