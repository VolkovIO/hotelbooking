package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.query.PageCriteria;
import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.UserId;
import java.util.Optional;

public interface BookingRepository {

  Booking save(Booking booking);

  Optional<Booking> findById(BookingId bookingId);

  PagedResult<Booking> findByUserId(UserId userId, PageCriteria pageCriteria);
}
