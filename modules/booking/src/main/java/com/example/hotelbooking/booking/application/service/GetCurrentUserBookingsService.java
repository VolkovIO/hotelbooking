package com.example.hotelbooking.booking.application.service;

import com.example.hotelbooking.booking.application.port.in.GetCurrentUserBookingsUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.query.GetCurrentUserBookingsQuery;
import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.domain.Booking;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCurrentUserBookingsService implements GetCurrentUserBookingsUseCase {

  private final BookingRepository bookingRepository;

  @Override
  public PagedResult<Booking> execute(GetCurrentUserBookingsQuery query) {
    Objects.requireNonNull(query, "query must not be null");

    log.debug(
        "Getting current user bookings: userId={}, page={}, size={}",
        query.userId(),
        query.pageCriteria().page(),
        query.pageCriteria().size());

    PagedResult<Booking> result =
        bookingRepository.findByUserId(query.userId(), query.pageCriteria());

    log.debug(
        "Current user bookings found: userId={}, page={}, size={}, totalElements={}, returned={}",
        query.userId(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.content().size());

    return result;
  }
}
