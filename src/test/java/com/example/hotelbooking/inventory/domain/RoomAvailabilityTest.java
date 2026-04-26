package com.example.hotelbooking.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomAvailabilityTest {

  @Test
  void shouldPlaceHoldAndDecreaseAvailableRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10);

    RoomAvailability updated = availability.placeHold(2);

    assertEquals(10, updated.getTotalRooms(), "totalRooms should remain unchanged");
    assertEquals(2, updated.getHeldRooms(), "heldRooms should increase after placing hold");
    assertEquals(
        0, updated.getBookedRooms(), "bookedRooms should remain unchanged after placing hold");
    assertEquals(8, updated.availableRooms(), "availableRooms should decrease after placing hold");
  }

  @Test
  void shouldReleaseHoldAndDecreaseHeldRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10)
            .placeHold(3);

    RoomAvailability updated = availability.releaseHold(2);

    assertEquals(1, updated.getHeldRooms(), "heldRooms should decrease after releasing hold");
    assertEquals(
        0, updated.getBookedRooms(), "bookedRooms should remain unchanged after releasing hold");
    assertEquals(
        9, updated.availableRooms(), "availableRooms should increase after releasing hold");
  }

  @Test
  void shouldConfirmHoldAndMoveHeldRoomsToBookedRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10)
            .placeHold(4);

    RoomAvailability updated = availability.confirmHold(3);

    assertEquals(1, updated.getHeldRooms(), "heldRooms should decrease after confirming hold");
    assertEquals(3, updated.getBookedRooms(), "bookedRooms should increase after confirming hold");
    assertEquals(
        6, updated.availableRooms(), "availableRooms should reflect booked and held rooms");
  }

  @Test
  void shouldThrowExceptionWhenPlacingHoldBeyondAvailableRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 2);

    assertThrows(
        InventoryDomainException.class,
        () -> availability.placeHold(3),
        "Placing hold beyond available rooms should throw InventoryDomainException");
  }

  @Test
  void shouldAdjustCapacityWhenNewTotalRoomsIsNotLowerThanOccupiedRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10)
            .placeHold(2)
            .confirmHold(1);
    // now: total=10, held=1, booked=1

    availability.adjustCapacity(5);

    assertEquals(5, availability.getTotalRooms(), "totalRooms should be updated");
    assertEquals(1, availability.getHeldRooms(), "heldRooms should remain unchanged");
    assertEquals(1, availability.getBookedRooms(), "bookedRooms should remain unchanged");
    assertEquals(3, availability.availableRooms(), "availableRooms should be recalculated");
  }

  @Test
  void shouldThrowExceptionWhenAdjustingCapacityBelowHeldAndBookedRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10)
            .placeHold(2)
            .confirmHold(1);
    // now: held=1, booked=1, occupied=2

    assertThrows(
        InventoryDomainException.class,
        () -> availability.adjustCapacity(1),
        "Adjusting capacity below held + booked rooms should throw InventoryDomainException");
  }

  @Test
  void shouldAllowAdjustingCapacityToExactOccupiedRoomsCount() {
    RoomAvailability availability =
        RoomAvailability.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10)
            .placeHold(3)
            .confirmHold(2);
    // held=1, booked=2, occupied=3

    availability.adjustCapacity(3);

    assertEquals(3, availability.getTotalRooms(), "totalRooms should be updated to occupied count");
    assertEquals(1, availability.getHeldRooms(), "heldRooms should remain unchanged");
    assertEquals(2, availability.getBookedRooms(), "bookedRooms should remain unchanged");
    assertEquals(0, availability.availableRooms(), "availableRooms should become zero");
  }

  @Test
  void shouldReleaseBookedRoomsAndIncreaseAvailableRooms() {
    RoomAvailability availability =
        RoomAvailability.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10)
            .placeHold(2)
            .confirmHold(2);

    RoomAvailability updated = availability.releaseBookedRooms(1);

    assertEquals(0, updated.getHeldRooms(), "heldRooms should remain unchanged");
    assertEquals(1, updated.getBookedRooms(), "bookedRooms should decrease");
    assertEquals(9, updated.availableRooms(), "availableRooms should increase");
  }

  @Test
  void shouldThrowExceptionWhenReleasingMoreBookedRoomsThanCurrentlyBooked() {
    RoomAvailability availability =
        RoomAvailability.create(
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1), 10);

    assertThrows(InventoryDomainException.class, () -> availability.releaseBookedRooms(1));
  }
}
