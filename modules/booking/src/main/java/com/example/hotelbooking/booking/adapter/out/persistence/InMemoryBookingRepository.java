package com.example.hotelbooking.booking.adapter.out.persistence;

import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.query.PageCriteria;
import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.UserId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("booking-in-memory")
class InMemoryBookingRepository implements BookingRepository {

  private final Map<BookingId, Booking> storage = new ConcurrentHashMap<>();

  @Override
  public Booking save(Booking booking) {
    storage.put(booking.getId(), booking);
    return booking;
  }

  @Override
  public Optional<Booking> findById(BookingId bookingId) {
    return Optional.ofNullable(storage.get(bookingId));
  }

  @Override
  public PagedResult<Booking> findByUserId(UserId userId, PageCriteria pageCriteria) {
    List<Booking> allUserBookings =
        storage.values().stream()
            .filter(booking -> booking.isOwnedBy(userId))
            .sorted(Comparator.comparing((Booking booking) -> booking.getId().value()).reversed())
            .toList();

    List<Booking> content =
        allUserBookings.stream().skip(pageCriteria.offset()).limit(pageCriteria.size()).toList();

    return new PagedResult<>(
        content, pageCriteria.page(), pageCriteria.size(), allUserBookings.size());
  }
}
