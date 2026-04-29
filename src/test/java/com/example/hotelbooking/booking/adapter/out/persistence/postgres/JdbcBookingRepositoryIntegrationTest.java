package com.example.hotelbooking.booking.adapter.out.persistence.postgres;

import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.BookingRepositoryContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
@JdbcTest
@Testcontainers
@ActiveProfiles("booking-postgres")
@Import({JdbcBookingRepository.class, LiquibaseAutoConfiguration.class})
class JdbcBookingRepositoryIntegrationTest implements BookingRepositoryContractTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @Autowired private JdbcBookingRepository jdbcRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanUp() {
    jdbcTemplate.update("delete from bookings");
  }

  @Override
  public BookingRepository repository() {
    return jdbcRepository;
  }
}
