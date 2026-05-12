package com.example.hotelbooking.bookingservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hotelbooking.booking.application.command.StartBookingSagaCommand;
import com.example.hotelbooking.booking.application.exception.RoomHoldFailedException;
import com.example.hotelbooking.booking.application.payment.PaymentAuthorizationRequest;
import com.example.hotelbooking.booking.application.payment.PaymentClientException;
import com.example.hotelbooking.booking.application.payment.PaymentResult;
import com.example.hotelbooking.booking.application.payment.PaymentStatus;
import com.example.hotelbooking.booking.application.port.in.StartBookingSagaUseCase;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryLookupPort;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.port.out.PaymentClient;
import com.example.hotelbooking.booking.application.port.out.RoomTypeReference;
import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStatus;
import com.example.hotelbooking.booking.application.security.CurrentUser;
import com.example.hotelbooking.booking.application.security.CurrentUserProvider;
import com.example.hotelbooking.booking.application.security.UserRole;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = {
      BookingServiceApplication.class,
      BookingSagaContentionIntegrationTest.DoublesConfiguration.class
    })
@ActiveProfiles({"booking-postgres", "outbox-logging"})
@Testcontainers
class BookingSagaContentionIntegrationTest {

  /*
   * This is a booking-service level contention test.
   *
   * The previous inventory integration test already verified the real MongoDB invariant:
   *
   *   one last available room -> only one inventory hold can be placed concurrently
   *
   * This test verifies the next level:
   *
   *   how booking saga reacts when many booking requests compete for one room
   *
   * Expected result:
   *
   * - exactly one saga completes;
   * - exactly one booking becomes CONFIRMED;
   * - losing sagas become FAILED;
   * - losing bookings become REJECTED;
   * - payment is authorized only for the winning booking.
   *
   * Important:
   *
   * This test does not start the real inventory-service.
   * InventoryReservationPort is replaced with a controlled test double that allows
   * exactly one hold and rejects all competing attempts.
   *
   * Real inventory atomic behavior is covered by InventoryLastRoomContentionIntegrationTest.
   */
  private static final int CLIENT_COUNT = 20;

  private static final UUID HOTEL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final LocalDate CHECK_IN = LocalDate.of(2035, 1, 10);
  private static final LocalDate CHECK_OUT = LocalDate.of(2035, 1, 11);

  private static final BigDecimal PAYMENT_AMOUNT = BigDecimal.valueOf(100);
  private static final String PAYMENT_CURRENCY = "USD";

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("hotelbooking")
          .withUsername("hotelbooking")
          .withPassword("hotelbooking");

  @Autowired private StartBookingSagaUseCase startBookingSagaUseCase;

  @Autowired private BookingRepository bookingRepository;

  @Autowired private ContendedInventoryReservationPort inventoryReservationPort;

  @Autowired private SuccessfulPaymentClient paymentClient;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void setUp() {
    inventoryReservationPort.reset();
    paymentClient.reset();
  }

  @Test
  void shouldConfirmOnlyOneBookingWhenManySagasCompeteForLastRoom() {
    /*
     * Step 1.
     *
     * Run many booking sagas concurrently.
     *
     * All commands target the same:
     *
     * - hotelId
     * - roomTypeId
     * - stay period
     *
     * The fake inventory adapter simulates exactly one last available room.
     */
    List<BookingSaga> sagas = runConcurrentBookingSagaAttempts();

    /*
     * Step 2.
     *
     * Load final Booking aggregates from PostgreSQL.
     *
     * The returned saga contains bookingId, but the business status lives
     * in the Booking aggregate.
     */
    List<Booking> bookings =
        sagas.stream()
            .map(saga -> bookingRepository.findById(new BookingId(saga.getBookingId())))
            .map(
                optional ->
                    optional.orElseThrow(() -> new IllegalStateException("Booking not found")))
            .toList();

    /*
     * Step 3.
     *
     * Count final saga statuses.
     */
    long completedSagas =
        sagas.stream().filter(saga -> saga.getStatus() == BookingSagaStatus.COMPLETED).count();

    long failedSagas =
        sagas.stream().filter(saga -> saga.getStatus() == BookingSagaStatus.FAILED).count();

    /*
     * Step 4.
     *
     * Count final booking statuses.
     */
    long confirmedBookings =
        bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count();

    long rejectedBookings =
        bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.REJECTED).count();

    /*
     * Final assertions.
     *
     * This is the booking-level invariant:
     *
     * - only one saga wins the inventory contention and completes;
     * - only one booking is confirmed;
     * - all other bookings are rejected;
     * - payment is called only for the winning booking.
     */
    assertThat(completedSagas).isEqualTo(1);
    assertThat(failedSagas).isEqualTo(CLIENT_COUNT - 1);

    assertThat(confirmedBookings).isEqualTo(1);
    assertThat(rejectedBookings).isEqualTo(CLIENT_COUNT - 1);

    assertThat(inventoryReservationPort.successfulHoldCount()).isEqualTo(1);
    assertThat(inventoryReservationPort.failedHoldCount()).isEqualTo(CLIENT_COUNT - 1);

    assertThat(paymentClient.authorizeCount()).isEqualTo(1);
    assertThat(paymentClient.approveCount()).isEqualTo(1);
    assertThat(paymentClient.cancelCount()).isZero();
  }

  private List<BookingSaga> runConcurrentBookingSagaAttempts() {
    /*
     * Java 21 ExecutorService is AutoCloseable, so try-with-resources closes
     * worker threads after the test.
     */
    try (ExecutorService executor = Executors.newFixedThreadPool(CLIENT_COUNT)) {
      /*
       * CountDownLatch aligns the start of all workers.
       *
       * Without it, requests may start one by one during submission,
       * reducing real contention.
       */
      CountDownLatch startLatch = new CountDownLatch(1);

      List<Callable<BookingSaga>> tasks =
          java.util.stream.IntStream.range(0, CLIENT_COUNT)
              .mapToObj(ignored -> bookingSagaAttempt(startLatch))
              .toList();

      List<Future<BookingSaga>> futures = tasks.stream().map(executor::submit).toList();

      /*
       * Release all workers at once.
       */
      startLatch.countDown();

      return futures.stream().map(this::completedSaga).toList();
    }
  }

  private Callable<BookingSaga> bookingSagaAttempt(CountDownLatch startLatch) {
    return () -> {
      startLatch.await(10, TimeUnit.SECONDS);

      return startBookingSagaUseCase.execute(
          new StartBookingSagaCommand(
              UserId.newId(),
              HOTEL_ID,
              ROOM_TYPE_ID,
              new StayPeriod(CHECK_IN, CHECK_OUT),
              1,
              PAYMENT_AMOUNT,
              PAYMENT_CURRENCY));
    };
  }

  private BookingSaga completedSaga(Future<BookingSaga> future) {
    try {
      return future.get(20, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();

      throw new IllegalStateException("Interrupted while waiting for booking saga", exception);
    } catch (ExecutionException exception) {
      throw new IllegalStateException("Booking saga failed unexpectedly", exception);
    } catch (TimeoutException exception) {
      throw new IllegalStateException("Booking saga did not finish", exception);
    }
  }

  @TestConfiguration
  static class DoublesConfiguration {

    @Bean
    @Primary
    CurrentUserProvider currentUserProvider() {
      return () -> new CurrentUser(UserId.newId(), Set.of(UserRole.USER, UserRole.ADMIN));
    }

    @Bean
    @Primary
    InventoryLookupPort inventoryLookupPort() {
      return (hotelId, roomTypeId) -> Optional.of(new RoomTypeReference(hotelId, roomTypeId, 2));
    }

    @Bean
    @Primary
    ContendedInventoryReservationPort contendedInventoryReservationPort() {
      return new ContendedInventoryReservationPort();
    }

    @Bean
    @Primary
    SuccessfulPaymentClient successfulPaymentClient() {
      return new SuccessfulPaymentClient();
    }
  }

  static final class ContendedInventoryReservationPort implements InventoryReservationPort {

    private final AtomicBoolean lastRoomAvailable = new AtomicBoolean(true);
    private final AtomicInteger successfulHolds = new AtomicInteger();
    private final AtomicInteger failedHolds = new AtomicInteger();

    void reset() {
      lastRoomAvailable.set(true);
      successfulHolds.set(0);
      failedHolds.set(0);
    }

    int successfulHoldCount() {
      return successfulHolds.get();
    }

    int failedHoldCount() {
      return failedHolds.get();
    }

    @Override
    public UUID placeHold(
        UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
      /*
       * Exactly one concurrent request can switch lastRoomAvailable from true to false.
       * All other requests simulate inventory business rejection.
       */
      if (lastRoomAvailable.compareAndSet(true, false)) {
        successfulHolds.incrementAndGet();
        return UUID.randomUUID();
      }

      failedHolds.incrementAndGet();
      throw new RoomHoldFailedException("Not enough rooms available to place hold");
    }

    @Override
    public void releaseHold(UUID holdId) {
      lastRoomAvailable.set(true);
    }

    @Override
    public void confirmHold(UUID holdId) {
      /*
       * No-op for this test.
       *
       * The fake inventory adapter only simulates contention on placeHold.
       * The inventory-level atomic update itself is covered by the previous
       * MongoDB Testcontainers test.
       */
    }

    @Override
    public void cancelConfirmedReservation(
        UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms) {
      /*
       * No-op for this test.
       */
    }
  }

  static final class SuccessfulPaymentClient implements PaymentClient {

    private final ConcurrentMap<UUID, PaymentResult> payments = new ConcurrentHashMap<>();
    private final AtomicInteger authorizationAttempts = new AtomicInteger();
    private final AtomicInteger approvalAttempts = new AtomicInteger();
    private final AtomicInteger cancellationAttempts = new AtomicInteger();

    void reset() {
      payments.clear();
      authorizationAttempts.set(0);
      approvalAttempts.set(0);
      cancellationAttempts.set(0);
    }

    int authorizeCount() {
      return authorizationAttempts.get();
    }

    int approveCount() {
      return approvalAttempts.get();
    }

    int cancelCount() {
      return cancellationAttempts.get();
    }

    @Override
    public PaymentResult authorize(PaymentAuthorizationRequest request) {
      Objects.requireNonNull(request.correlationId(), "correlationId must not be null");

      authorizationAttempts.incrementAndGet();

      UUID paymentId = UUID.randomUUID();

      PaymentResult result =
          new PaymentResult(
              paymentId,
              request.bookingId(),
              request.userId().value(),
              request.amount(),
              request.currency(),
              PaymentStatus.AUTHORIZED,
              "test-provider",
              "test-provider-payment-" + paymentId,
              null);

      payments.put(paymentId, result);

      return result;
    }

    @Override
    public PaymentResult approve(UUID paymentId, UUID correlationId) {
      Objects.requireNonNull(correlationId, "correlationId must not be null");

      approvalAttempts.incrementAndGet();

      PaymentResult authorized = payments.get(paymentId);

      if (authorized == null) {
        throw new PaymentClientException("Payment was not authorized: " + paymentId);
      }

      PaymentResult approved =
          new PaymentResult(
              authorized.paymentId(),
              authorized.bookingId(),
              authorized.userId(),
              authorized.amount(),
              authorized.currency(),
              PaymentStatus.APPROVED,
              authorized.provider(),
              authorized.providerPaymentId(),
              null);

      payments.put(paymentId, approved);

      return approved;
    }

    @Override
    public PaymentResult cancel(UUID paymentId, UUID correlationId) {
      Objects.requireNonNull(correlationId, "correlationId must not be null");

      cancellationAttempts.incrementAndGet();

      PaymentResult authorized = payments.get(paymentId);

      if (authorized == null) {
        throw new PaymentClientException("Payment was not authorized: " + paymentId);
      }

      PaymentResult cancelled =
          new PaymentResult(
              authorized.paymentId(),
              authorized.bookingId(),
              authorized.userId(),
              authorized.amount(),
              authorized.currency(),
              PaymentStatus.CANCELLED,
              authorized.provider(),
              authorized.providerPaymentId(),
              "Cancelled by test double");

      payments.put(paymentId, cancelled);

      return cancelled;
    }
  }
}
