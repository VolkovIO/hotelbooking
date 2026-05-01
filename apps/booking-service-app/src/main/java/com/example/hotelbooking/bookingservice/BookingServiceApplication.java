package com.example.hotelbooking.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.hotelbooking.booking")
public class BookingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BookingServiceApplication.class, args);
  }
}
