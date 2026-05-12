package com.example.hotelbooking.booking.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingLifecycleEventTest {

  private static final UUID HOTEL_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID HOLD_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID CORRELATION_ID =
      UUID.fromString("50000000-0000-0000-0000-000000000001");
  private static final UUID CAUSATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

  @Test
  void shouldUseProvidedCorrelationId() {
    Booking booking = bookingOnHold();

    BookingLifecycleEvent event =
        BookingLifecycleEvent.placedOnHold(booking, CORRELATION_ID, CAUSATION_ID);

    assertThat(event.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(event.causationId()).isEqualTo(CAUSATION_ID);
  }

  @Test
  void shouldRejectMissingCorrelationId() {
    Booking booking = bookingOnHold();

    assertThatThrownBy(() -> BookingLifecycleEvent.placedOnHold(booking, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("correlationId must not be null");
  }

  private Booking bookingOnHold() {
    Booking booking =
        Booking.create(
            new UserId(USER_ID),
            HOTEL_ID,
            ROOM_TYPE_ID,
            new StayPeriod(LocalDate.of(2035, 1, 10), LocalDate.of(2035, 1, 11)),
            1);

    booking.placeOnHold(HOLD_ID);

    return booking;
  }
}
