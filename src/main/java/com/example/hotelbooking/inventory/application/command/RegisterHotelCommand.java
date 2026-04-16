package com.example.hotelbooking.inventory.application.command;

import java.util.Objects;

public record RegisterHotelCommand(String name, String city) {

  public RegisterHotelCommand {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(city, "city must not be null");
  }
}
