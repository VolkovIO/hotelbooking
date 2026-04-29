package com.example.hotelbooking.booking.adapter.out.persistence.postgres;

import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.booking.domain.StayPeriod;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("booking-postgres")
@RequiredArgsConstructor
class JdbcBookingRepository implements BookingRepository {

  private final JdbcClient jdbcClient;

  // Simple upsert implementation.
  // In production, it's better to use native PostgreSQL "ON CONFLICT"
  // or check for existence to avoid exception overhead.
  @Override
  public Booking save(Booking booking) {
    try {
      insert(booking);
    } catch (DuplicateKeyException exception) {
      update(booking);
    }

    return booking;
  }

  @Override
  public Optional<Booking> findById(BookingId bookingId) {
    return jdbcClient
        .sql(
            """
            select id,
                   hotel_id,
                   room_type_id,
                   check_in,
                   check_out,
                   guest_count,
                   status,
                   hold_id
              from bookings
             where id = :id
            """)
        .param("id", bookingId.value())
        .query(this::mapRow)
        .optional();
  }

  private void insert(Booking booking) {
    jdbcClient
        .sql(
            """
            insert into bookings (
                id,
                hotel_id,
                room_type_id,
                check_in,
                check_out,
                guest_count,
                status,
                hold_id
            )
            values (
                :id,
                :hotelId,
                :roomTypeId,
                :checkIn,
                :checkOut,
                :guestCount,
                :status,
                :holdId
            )
            """)
        .param("id", booking.getId().value())
        .param("hotelId", booking.getHotelId())
        .param("roomTypeId", booking.getRoomTypeId())
        .param("checkIn", booking.getStayPeriod().checkIn())
        .param("checkOut", booking.getStayPeriod().checkOut())
        .param("guestCount", booking.getGuestCount())
        .param("status", booking.getStatus().name())
        .param("holdId", booking.getHoldId())
        .update();
  }

  private void update(Booking booking) {
    jdbcClient
        .sql(
            """
            update bookings
               set hotel_id = :hotelId,
                   room_type_id = :roomTypeId,
                   check_in = :checkIn,
                   check_out = :checkOut,
                   guest_count = :guestCount,
                   status = :status,
                   hold_id = :holdId,
                   updated_at = CURRENT_TIMESTAMP
             where id = :id
            """)
        .param("id", booking.getId().value())
        .param("hotelId", booking.getHotelId())
        .param("roomTypeId", booking.getRoomTypeId())
        .param("checkIn", booking.getStayPeriod().checkIn())
        .param("checkOut", booking.getStayPeriod().checkOut())
        .param("guestCount", booking.getGuestCount())
        .param("status", booking.getStatus().name())
        .param("holdId", booking.getHoldId())
        .update();
  }

  @SuppressWarnings("unused")
  private Booking mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
    UUID holdId = resultSet.getObject("hold_id", UUID.class);

    return Booking.restore(
        new BookingId(resultSet.getObject("id", UUID.class)),
        resultSet.getObject("hotel_id", UUID.class),
        resultSet.getObject("room_type_id", UUID.class),
        new StayPeriod(
            resultSet.getDate("check_in").toLocalDate(),
            resultSet.getDate("check_out").toLocalDate()),
        resultSet.getInt("guest_count"),
        BookingStatus.valueOf(resultSet.getString("status")),
        holdId);
  }
}
