package com.example.hotelbooking.bootstrap;

import com.example.hotelbooking.booking.application.command.ConfirmBookingCommand;
import com.example.hotelbooking.booking.application.command.ConfirmBookingUseCase;
import com.example.hotelbooking.booking.application.command.CreateBookingCommand;
import com.example.hotelbooking.booking.application.command.CreateBookingUseCase;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.command.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.command.InitializeRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.command.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("in-memory")
@RequiredArgsConstructor
public class InMemoryDataInitializer implements ApplicationRunner {

  private final RegisterHotelUseCase registerHotelUseCase;
  private final AddRoomTypeUseCase addRoomTypeUseCase;
  private final InitializeRoomAvailabilityUseCase initializeRoomAvailabilityUseCase;
  private final CreateBookingUseCase createBookingUseCase;
  private final ConfirmBookingUseCase confirmBookingUseCase;

  @Override
  public void run(ApplicationArguments args) {
    Hotel hotel = registerHotelUseCase.execute(new RegisterHotelCommand("Demo Hotel", "Kazan"));

    Hotel hotelAfterStandard =
        addRoomTypeUseCase.execute(new AddRoomTypeCommand(hotel.getId(), "STANDARD", 2));

    Hotel hotelAfterLux =
        addRoomTypeUseCase.execute(new AddRoomTypeCommand(hotel.getId(), "LUX", 4));

    RoomType standardRoomType =
        hotelAfterStandard.getRoomTypes().stream()
            .filter(roomType -> "STANDARD".equals(roomType.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("STANDARD room type not found"));

    RoomType luxRoomType =
        hotelAfterLux.getRoomTypes().stream()
            .filter(roomType -> "LUX".equals(roomType.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("LUX room type not found"));

    initializeRoomAvailabilityUseCase.execute(
        new RoomAvailabilityPeriodCommand(
            hotel.getId(),
            standardRoomType.getId(),
            LocalDate.of(2030, 6, 1),
            LocalDate.of(2030, 6, 30),
            6));

    initializeRoomAvailabilityUseCase.execute(
        new RoomAvailabilityPeriodCommand(
            hotel.getId(),
            luxRoomType.getId(),
            LocalDate.of(2030, 6, 1),
            LocalDate.of(2030, 6, 30),
            2));

    Booking booking =
        createBookingUseCase.execute(
            new CreateBookingCommand(
                hotel.getId(),
                standardRoomType.getId(),
                LocalDate.of(2030, 6, 10),
                LocalDate.of(2030, 6, 20),
                2));
    Booking confirmedBooking =
        confirmBookingUseCase.execute(new ConfirmBookingCommand(booking.getId()));

    log.info("=== In-memory demo data initialized ===");
    log.info("Hotel id: {}", hotel.getId());
    log.info("STANDARD room type id: {}", standardRoomType.getId());
    log.info("LUX room type id: {}", luxRoomType.getId());
    log.info("Booking id: {}", booking.getId());
    log.info("Booking status: {}", confirmedBooking.getStatus());
  }
}
