package com.example.hotelbooking.booking.application.port.in;

import com.example.hotelbooking.booking.application.query.GetCurrentUserBookingsQuery;
import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.domain.Booking;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetCurrentUserBookingsUseCase {

  PagedResult<Booking> execute(GetCurrentUserBookingsQuery query);
}
