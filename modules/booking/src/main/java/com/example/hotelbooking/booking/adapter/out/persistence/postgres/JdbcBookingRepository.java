package com.example.hotelbooking.booking.adapter.out.persistence.postgres;

import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("booking-postgres")
@RequiredArgsConstructor
class JdbcBookingRepository implements BookingRepository {

  private final JdbcClient jdbcClient;

  @Override
  public Booking save(Booking booking) {
    jdbcClient
        .sql(
            """
            insert into bookings (
                id,
                user_id,
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
                :userId,
                :hotelId,
                :roomTypeId,
                :checkIn,
                :checkOut,
                :guestCount,
                :status,
                :holdId
            )
            on conflict (id) do update
               set user_id = excluded.user_id,
                   hotel_id = excluded.hotel_id,
                   room_type_id = excluded.room_type_id,
                   check_in = excluded.check_in,
                   check_out = excluded.check_out,
                   guest_count = excluded.guest_count,
                   status = excluded.status,
                   hold_id = excluded.hold_id,
                   updated_at = CURRENT_TIMESTAMP
            """)
        .param("id", booking.getId().value())
        .param("userId", booking.getUserId().value())
        .param("hotelId", booking.getHotelId())
        .param("roomTypeId", booking.getRoomTypeId())
        .param("checkIn", booking.getStayPeriod().checkIn())
        .param("checkOut", booking.getStayPeriod().checkOut())
        .param("guestCount", booking.getGuestCount())
        .param("status", booking.getStatus().name())
        .param("holdId", booking.getHoldId())
        .update();

    return booking;
  }

  @Override
  public Optional<Booking> findById(BookingId bookingId) {
    return jdbcClient
        .sql(
            """
            select id,
                   user_id,
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

  @SuppressWarnings("unused")
  private Booking mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
    UUID holdId = resultSet.getObject("hold_id", UUID.class);

    return Booking.restore(
        new BookingId(resultSet.getObject("id", UUID.class)),
        new UserId(resultSet.getObject("user_id", UUID.class)),
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
