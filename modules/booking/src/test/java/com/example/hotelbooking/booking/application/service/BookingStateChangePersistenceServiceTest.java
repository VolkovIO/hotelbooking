package com.example.hotelbooking.booking.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.event.BookingLifecycleEvent;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxRepository;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

class BookingStateChangePersistenceServiceTest {

  private final BookingRepository bookingRepository =
      org.mockito.Mockito.mock(BookingRepository.class);

  private final BookingOutboxRepository bookingOutboxRepository =
      org.mockito.Mockito.mock(BookingOutboxRepository.class);

  private final BookingStateChangePersistenceService service =
      new BookingStateChangePersistenceService(bookingRepository, bookingOutboxRepository);

  @Test
  void shouldSaveBookingBeforeOutboxEvent() {
    Booking booking = bookingOnHold();
    BookingLifecycleEvent event = BookingLifecycleEvent.placedOnHold(booking);

    when(bookingRepository.save(booking)).thenReturn(booking);

    Booking result = service.persist(booking, event);

    assertSame(booking, result);

    InOrder inOrder = inOrder(bookingRepository, bookingOutboxRepository);
    inOrder.verify(bookingRepository).save(booking);
    inOrder.verify(bookingOutboxRepository).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldDefineTransactionalBoundaryForBookingAndOutboxWrite() throws NoSuchMethodException {
    Method method =
        BookingStateChangePersistenceService.class.getDeclaredMethod(
            "persist", Booking.class, BookingLifecycleEvent.class);

    assertTrue(method.isAnnotationPresent(Transactional.class));
  }

  private Booking bookingOnHold() {
    Booking booking =
        Booking.create(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new StayPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20)),
            2);

    booking.placeOnHold(UUID.randomUUID());

    return booking;
  }
}
