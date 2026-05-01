package com.example.hotelbooking.inventory.application.port.in;

import java.util.List;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface FindHotelsUseCase {

  List<HotelSummaryResult> execute(int limit);
}
