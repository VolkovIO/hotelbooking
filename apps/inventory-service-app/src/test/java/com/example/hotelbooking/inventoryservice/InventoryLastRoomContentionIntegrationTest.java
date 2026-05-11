package com.example.hotelbooking.inventoryservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hotelbooking.inventory.application.command.AddRoomTypeCommand;
import com.example.hotelbooking.inventory.application.command.RegisterHotelCommand;
import com.example.hotelbooking.inventory.application.command.RoomAvailabilityPeriodCommand;
import com.example.hotelbooking.inventory.application.port.in.AddRoomTypeUseCase;
import com.example.hotelbooking.inventory.application.port.in.GetRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.port.in.InitializeRoomAvailabilityUseCase;
import com.example.hotelbooking.inventory.application.port.in.InventoryReservationUseCase;
import com.example.hotelbooking.inventory.application.port.in.RegisterHotelUseCase;
import com.example.hotelbooking.inventory.domain.Hotel;
import com.example.hotelbooking.inventory.domain.RoomAvailability;
import com.example.hotelbooking.inventory.domain.RoomType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = InventoryServiceApplication.class)
@ActiveProfiles("inventory-mongo")
@Testcontainers
class InventoryLastRoomContentionIntegrationTest {

  /*
   * This test simulates a real "last room contention" scenario.
   *
   * Business case:
   * - there is exactly 1 available room for a room type and stay date;
   * - many clients try to place a hold for that same room at the same time;
   * - only one hold must succeed;
   * - all other attempts must fail;
   * - inventory must not overbook or over-hold the room.
   *
   * This is intentionally an app-level integration test, not a module unit test,
   * because the concurrency problem usually lives in the persistence boundary:
   *
   *   read availability -> calculate new values -> save availability
   *
   * If two threads read the same old MongoDB state concurrently, both may think
   * that the room is still available. That kind of race condition cannot be
   * reliably detected with a pure domain unit test.
   */

  private static final int CLIENT_COUNT = 20;

  /*
   * We intentionally configure only one room.
   * This makes the invariant very strict:
   *
   *   successful holds must be exactly 1
   */
  private static final int LAST_AVAILABLE_ROOM = 1;

  /*
   * In the current booking model one booking reserves one room.
   */
  private static final int ROOMS_PER_BOOKING = 1;

  /*
   * Testcontainers starts a real MongoDB container for this test.
   *
   * This is important because we want to test real persistence behavior,
   * including concurrent writes and MongoDB update semantics.
   */
  @Container private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

  @Autowired private MongoTemplate mongoTemplate;

  @Autowired private RegisterHotelUseCase registerHotelUseCase;

  @Autowired private AddRoomTypeUseCase addRoomTypeUseCase;

  @Autowired private InitializeRoomAvailabilityUseCase initializeRoomAvailabilityUseCase;

  @Autowired private InventoryReservationUseCase inventoryReservationUseCase;

  @Autowired private GetRoomAvailabilityUseCase getRoomAvailabilityUseCase;

  /*
   * Spring Boot must use the MongoDB URI from Testcontainers.
   *
   * MONGO::getReplicaSetUrl returns a connection string for the running
   * MongoDB test container.
   */
  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
  }

  /*
   * Clean the database before every test.
   *
   * This keeps the test isolated:
   * - no hotels from previous tests;
   * - no room types from previous tests;
   * - no availability records from previous tests;
   * - no active holds from previous tests.
   */
  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().drop();
  }

  @Test
  void shouldAllowOnlyOneConcurrentHoldForLastAvailableRoom() throws Exception {
    /*
     * Step 1.
     *
     * Create a hotel through the real application use case.
     *
     * We do not insert MongoDB documents directly because this test should
     * verify the service-level behavior through application ports.
     */
    Hotel hotel =
        registerHotelUseCase.execute(new RegisterHotelCommand("Contention Hotel", "Kazan"));

    /*
     * Step 2.
     *
     * Add one room type to the hotel.
     *
     * guestCapacity=2 is not important for this test.
     * The important part is that all concurrent clients will target the same
     * hotelId + roomTypeId + date range.
     */
    Hotel hotelWithRoomType =
        addRoomTypeUseCase.execute(new AddRoomTypeCommand(hotel.getId(), "Standard", 2));

    UUID roomTypeId = singleRoomTypeId(hotelWithRoomType);

    /*
     * Step 3.
     *
     * Use a future date to avoid conflicts with seed/demo data.
     *
     * checkOut is exclusive in hotel booking systems:
     *
     *   checkIn  = 2035-01-10
     *   checkOut = 2035-01-11
     *
     * means one night: January 10.
     */
    LocalDate checkIn = LocalDate.of(2035, 1, 10);
    LocalDate checkOut = LocalDate.of(2035, 1, 11);

    /*
     * Step 4.
     *
     * Initialize availability for exactly one day with exactly one room.
     *
     * This creates the "last room" situation:
     *
     *   totalRooms = 1
     *   heldRooms = 0
     *   bookedRooms = 0
     *   availableRooms = 1
     */
    initializeRoomAvailabilityUseCase.execute(
        new RoomAvailabilityPeriodCommand(
            hotel.getId(), roomTypeId, checkIn, checkIn, LAST_AVAILABLE_ROOM));

    /*
     * Step 5.
     *
     * Start many clients concurrently.
     *
     * Each client tries to place a hold for the same:
     *
     *   hotelId
     *   roomTypeId
     *   checkIn/checkOut
     *
     * The helper returns true for a successful hold and false for a rejected one.
     */
    List<Boolean> results = runConcurrentHoldAttempts(hotel.getId(), roomTypeId, checkIn, checkOut);

    /*
     * Step 6.
     *
     * Count successful hold attempts.
     *
     * The core business invariant:
     *
     *   with only 1 available room, only 1 hold may succeed
     */
    long successfulHolds = results.stream().filter(Boolean::booleanValue).count();

    /*
     * Step 7.
     *
     * Read current availability from the real use case.
     *
     * This verifies final persisted state, not only returned results.
     */
    List<RoomAvailability> availability =
        getRoomAvailabilityUseCase.execute(hotel.getId(), roomTypeId, checkIn, checkIn);

    /*
     * Final assertions.
     *
     * We assert both:
     *
     * - external behavior:
     *     exactly one client succeeded;
     *
     * - persisted inventory state:
     *     exactly one room is held;
     *     zero rooms remain available.
     *
     * If the implementation has a race condition, this test may fail with:
     *
     *   successfulHolds > 1
     *
     * or with inconsistent persisted availability.
     */
    assertThat(successfulHolds).isEqualTo(1);
    assertThat(availability).hasSize(1);
    assertThat(availability.getFirst().getHeldRooms()).isEqualTo(1);
    assertThat(availability.getFirst().availableRooms()).isZero();
  }

  private List<Boolean> runConcurrentHoldAttempts(
      UUID hotelId, UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) throws Exception {
    /*
     * A fixed thread pool represents many clients calling inventory at the same time.
     *
     * CLIENT_COUNT=20 is enough to expose a race condition without making the
     * test too heavy.
     */
    var executor = Executors.newFixedThreadPool(CLIENT_COUNT);

    /*
     * CountDownLatch is used to align the start of all threads.
     *
     * Without this latch, tasks may start one by one as they are submitted,
     * which reduces real contention.
     *
     * With this latch:
     *
     * - all tasks are submitted first;
     * - all tasks wait on startLatch.await(...);
     * - the test calls startLatch.countDown();
     * - all tasks start competing almost at the same time.
     */
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      /*
       * Build CLIENT_COUNT independent hold attempts.
       *
       * Each Callable<Boolean> returns:
       *
       * - true  if hold was placed successfully;
       * - false if inventory rejected the request.
       */
      List<Callable<Boolean>> tasks =
          java.util.stream.IntStream.range(0, CLIENT_COUNT)
              .mapToObj(ignored -> holdAttempt(hotelId, roomTypeId, checkIn, checkOut, startLatch))
              .toList();

      /*
       * Submit all tasks to the executor.
       *
       * At this point tasks are created and scheduled, but they are blocked
       * on startLatch.await(...).
       */
      var futures = tasks.stream().map(executor::submit).toList();

      /*
       * Release all workers at once.
       *
       * This is the actual moment when the concurrent race starts.
       */
      startLatch.countDown();

      /*
       * Collect all results.
       *
       * The timeout prevents the test from hanging forever if a worker gets stuck.
       */
      return futures.stream()
          .map(
              future -> {
                try {
                  return future.get(10, TimeUnit.SECONDS);
                } catch (Exception exception) {
                  throw new IllegalStateException(
                      "Concurrent hold attempt did not finish", exception);
                }
              })
          .toList();
    } finally {
      /*
       * Always stop executor threads, even when the test fails.
       */
      executor.shutdownNow();
    }
  }

  private Callable<Boolean> holdAttempt(
      UUID hotelId,
      UUID roomTypeId,
      LocalDate checkIn,
      LocalDate checkOut,
      CountDownLatch startLatch) {
    return () -> {
      /*
       * Wait until all concurrent clients are ready.
       *
       * If something goes wrong and the latch is not released, the timeout
       * prevents this worker from waiting forever.
       */
      startLatch.await(10, TimeUnit.SECONDS);

      try {
        /*
         * This is the operation under test.
         *
         * All workers call the same use case with the same hotel, room type,
         * date range and requested rooms.
         */
        inventoryReservationUseCase.placeHold(
            hotelId, roomTypeId, checkIn, checkOut, ROOMS_PER_BOOKING);

        return true;
      } catch (RuntimeException exception) {
        /*
         * Rejected hold attempts are expected.
         *
         * In this scenario 19 out of 20 clients should fail because only one
         * room is available.
         *
         * We intentionally do not assert the exact exception type here because
         * this test focuses on the concurrency invariant:
         *
         *   no more than one successful hold
         */
        return false;
      }
    };
  }

  private UUID singleRoomTypeId(Hotel hotel) {
    /*
     * The test creates exactly one room type, so it is safe to take the first one.
     */
    return hotel.getRoomTypes().stream().map(RoomType::getId).findFirst().orElseThrow();
  }
}
