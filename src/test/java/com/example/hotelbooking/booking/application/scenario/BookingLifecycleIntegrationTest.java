package com.example.hotelbooking.booking.application.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.hotelbooking.booking.application.command.CancelBookingCommand;
import com.example.hotelbooking.booking.application.command.ConfirmBookingCommand;
import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.port.in.CancelBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.ConfirmBookingUseCase;
import com.example.hotelbooking.booking.application.port.in.CreateBookingUseCase;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingDomainException;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;
import com.example.hotelbooking.inventory.application.port.in.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.port.in.GetRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.port.in.InitializeRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.port.in.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application-level integration tests for the booking lifecycle.
 *
 * <p>These tests intentionally use both booking and inventory application use cases with the
 * in-memory profile. They verify the behavior of the modular monolith as a whole, not isolated
 * domain objects.
 *
 * <p>The goal is to protect the booking-inventory lifecycle before introducing real persistence.
 *
 * <p>Covered flow:
 *
 * <ul>
 *   <li>creating a booking places an inventory hold;
 *   <li>cancelling an ON_HOLD booking releases the hold;
 *   <li>confirming a booking converts held rooms to booked rooms;
 *   <li>cancelling a CONFIRMED booking releases booked rooms;
 *   <li>invalid repeated transitions are rejected.
 * </ul>
 *
 * <p>This test should remain independent from concrete persistence implementations. After MongoDB
 * or PostgreSQL adapters are added, the same scenarios should continue to pass through application
 * ports and use cases.
 */
@DisplayName("Booking lifecycle scenarios")
@SpringBootTest
@ActiveProfiles("in-memory")
class BookingLifecycleIntegrationTest {

  private static final LocalDate CHECK_IN = LocalDate.of(2030, 6, 10);
  private static final LocalDate CHECK_OUT = LocalDate.of(2030, 6, 12);
  private static final int TOTAL_ROOMS = 10;
  private static final int GUEST_COUNT = 2;

  @Autowired private RegisterHotelUseCase registerHotelUseCase;

  @Autowired private AddRoomTypeUseCase addRoomTypeUseCase;

  @Autowired private InitializeRoomAvailabilityUseCase initializeRoomAvailabilityUseCase;

  @Autowired private GetRoomAvailabilityUseCase getRoomAvailabilityUseCase;

  @Autowired private CreateBookingUseCase createBookingUseCase;

  @Autowired private ConfirmBookingUseCase confirmBookingUseCase;

  @Autowired private CancelBookingUseCase cancelBookingUseCase;

  @Test
  void shouldCreateBookingAndPlaceInventoryHold() {
    TestInventory inventory = prepareInventory();

    Booking booking = createBooking(inventory);

    assertEquals(BookingStatus.ON_HOLD, booking.getStatus());

    List<RoomAvailability> availability = getAvailability(inventory);

    assertAvailability(availability, 9, 1, 0);
  }

  @Test
  void shouldCancelHeldBookingAndReleaseInventoryHold() {
    TestInventory inventory = prepareInventory();

    Booking booking = createBooking(inventory);

    Booking cancelledBooking =
        cancelBookingUseCase.execute(new CancelBookingCommand(booking.getId()));

    assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());

    List<RoomAvailability> availability = getAvailability(inventory);

    assertAvailability(availability, 10, 0, 0);
  }

  @Test
  void shouldConfirmBookingAndConvertHeldRoomsToBookedRooms() {
    TestInventory inventory = prepareInventory();

    Booking booking = createBooking(inventory);

    Booking confirmedBooking =
        confirmBookingUseCase.execute(new ConfirmBookingCommand(booking.getId()));

    assertEquals(BookingStatus.CONFIRMED, confirmedBooking.getStatus());

    List<RoomAvailability> availability = getAvailability(inventory);

    assertAvailability(availability, 9, 0, 1);
  }

  @Test
  void shouldCancelConfirmedBookingAndReleaseBookedRooms() {
    TestInventory inventory = prepareInventory();

    Booking booking = createBooking(inventory);
    Booking confirmedBooking =
        confirmBookingUseCase.execute(new ConfirmBookingCommand(booking.getId()));

    Booking cancelledBooking =
        cancelBookingUseCase.execute(new CancelBookingCommand(confirmedBooking.getId()));

    assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());

    List<RoomAvailability> availability = getAvailability(inventory);

    assertAvailability(availability, 10, 0, 0);
  }

  @Test
  void shouldRejectCancellingAlreadyCancelledBooking() {
    TestInventory inventory = prepareInventory();

    Booking booking = createBooking(inventory);
    Booking cancelledBooking =
        cancelBookingUseCase.execute(new CancelBookingCommand(booking.getId()));

    assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());

    assertThrows(
        BookingDomainException.class,
        () -> cancelBookingUseCase.execute(new CancelBookingCommand(cancelledBooking.getId())));
  }

  @Test
  void shouldRejectConfirmingCancelledBooking() {
    TestInventory inventory = prepareInventory();

    Booking booking = createBooking(inventory);
    Booking cancelledBooking =
        cancelBookingUseCase.execute(new CancelBookingCommand(booking.getId()));

    assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());

    assertThrows(
        BookingDomainException.class,
        () -> confirmBookingUseCase.execute(new ConfirmBookingCommand(cancelledBooking.getId())));
  }

  private TestInventory prepareInventory() {
    Hotel registeredHotel =
        registerHotelUseCase.execute(
            new RegisterHotelCommand("Scenario Hotel " + UUID.randomUUID(), "Kazan"));

    Hotel hotelWithRoomType =
        addRoomTypeUseCase.execute(
            new AddRoomTypeCommand(registeredHotel.getId(), "Standard", GUEST_COUNT));

    RoomType roomType = hotelWithRoomType.getRoomTypes().getFirst();

    initializeRoomAvailabilityUseCase.execute(
        new RoomAvailabilityPeriodCommand(
            registeredHotel.getId(),
            roomType.getId(),
            CHECK_IN,
            CHECK_OUT.minusDays(1),
            TOTAL_ROOMS));

    return new TestInventory(registeredHotel.getId(), roomType.getId());
  }

  private Booking createBooking(TestInventory inventory) {
    return createBookingUseCase.execute(
        new CreateBookingCommand(
            inventory.hotelId(), inventory.roomTypeId(), CHECK_IN, CHECK_OUT, GUEST_COUNT));
  }

  private List<RoomAvailability> getAvailability(TestInventory inventory) {
    return getRoomAvailabilityUseCase.execute(
        inventory.hotelId(), inventory.roomTypeId(), CHECK_IN, CHECK_OUT.minusDays(1));
  }

  private static void assertAvailability(
      List<RoomAvailability> availability, int availableRooms, int heldRooms, int bookedRooms) {

    assertEquals(2, availability.size());

    for (RoomAvailability item : availability) {
      assertEquals(availableRooms, item.availableRooms());
      assertEquals(heldRooms, item.getHeldRooms());
      assertEquals(bookedRooms, item.getBookedRooms());
    }
  }

  private record TestInventory(UUID hotelId, UUID roomTypeId) {}
}
