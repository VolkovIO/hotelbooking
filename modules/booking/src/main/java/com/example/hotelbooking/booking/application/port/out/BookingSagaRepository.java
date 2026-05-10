package com.example.hotelbooking.booking.application.port.out;

import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingSagaRepository {

  BookingSaga save(BookingSaga saga);

  Optional<BookingSaga> findById(BookingSagaId sagaId);

  Optional<BookingSaga> findByBookingId(UUID bookingId);

  List<BookingSaga> findReadyForRetry(Instant now, int batchSize);
}
