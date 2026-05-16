package com.example.hotelbooking.booking.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.query.GetCurrentUserBookingsQuery;
import com.example.hotelbooking.booking.application.query.PageCriteria;
import com.example.hotelbooking.booking.application.query.PagedResult;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserBookingsServiceTest {

  @Mock private BookingRepository bookingRepository;

  @InjectMocks private GetCurrentUserBookingsService service;

  @Test
  void shouldReturnCurrentUserBookingsPage() {
    UserId userId = userId();
    PageCriteria pageCriteria = new PageCriteria(0, 20);
    List<Booking> bookings = List.of(booking(userId), booking(userId));
    PagedResult<Booking> expectedResult = new PagedResult<>(bookings, 0, 20, 2);

    when(bookingRepository.findByUserId(userId, pageCriteria)).thenReturn(expectedResult);

    PagedResult<Booking> result =
        service.execute(new GetCurrentUserBookingsQuery(userId, pageCriteria));

    assertEquals(expectedResult, result);
    verify(bookingRepository).findByUserId(userId, pageCriteria);
  }

  @Test
  void shouldRejectNullQuery() {
    assertThrows(NullPointerException.class, () -> service.execute(null));
  }

  private Booking booking(UserId userId) {
    return Booking.create(
        userId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        new StayPeriod(LocalDate.of(2030, 6, 11), LocalDate.of(2030, 6, 12)),
        1);
  }

  private UserId userId() {
    return new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  }
}
