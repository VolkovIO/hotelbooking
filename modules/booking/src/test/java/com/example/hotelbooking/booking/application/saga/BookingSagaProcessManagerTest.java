package com.example.hotelbooking.booking.application.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hotelbooking.booking.application.payment.PaymentAuthorizationRequest;
import com.example.hotelbooking.booking.application.payment.PaymentClientException;
import com.example.hotelbooking.booking.application.payment.PaymentResult;
import com.example.hotelbooking.booking.application.payment.PaymentStatus;
import com.example.hotelbooking.booking.application.port.out.BookingRepository;
import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import com.example.hotelbooking.booking.application.port.out.InventoryReservationPort;
import com.example.hotelbooking.booking.application.port.out.PaymentClient;
import com.example.hotelbooking.booking.application.saga.action.ApprovePaymentSagaAction;
import com.example.hotelbooking.booking.application.saga.action.AuthorizePaymentSagaAction;
import com.example.hotelbooking.booking.application.saga.action.BookingSagaActionRegistry;
import com.example.hotelbooking.booking.application.saga.action.BookingSagaBookingLoader;
import com.example.hotelbooking.booking.application.saga.action.CancelBookingSagaAction;
import com.example.hotelbooking.booking.application.saga.action.CancelPaymentSagaAction;
import com.example.hotelbooking.booking.application.saga.action.ConfirmBookingSagaAction;
import com.example.hotelbooking.booking.application.saga.action.HoldInventorySagaAction;
import com.example.hotelbooking.booking.application.saga.action.ReleaseInventorySagaAction;
import com.example.hotelbooking.booking.application.service.BookingStateChangePersistenceService;
import com.example.hotelbooking.booking.domain.Booking;
import com.example.hotelbooking.booking.domain.BookingId;
import com.example.hotelbooking.booking.domain.BookingStatus;
import com.example.hotelbooking.booking.domain.StayPeriod;
import com.example.hotelbooking.booking.domain.UserId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingSagaProcessManagerTest {

  private static final UUID USER_ID_VALUE = UUID.fromString("2e1ecd64-e449-49a0-8744-eb5473c8e76b");
  private static final UserId USER_ID = new UserId(USER_ID_VALUE);
  private static final UUID HOTEL_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID HOLD_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID PAYMENT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final StayPeriod STAY_PERIOD =
      new StayPeriod(LocalDate.of(2030, 6, 27), LocalDate.of(2030, 6, 28));
  private static final int GUEST_COUNT = 1;
  private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("3500.00");
  private static final String PAYMENT_CURRENCY = "RUB";

  private BookingSagaRetryProperties retryProperties;

  @Mock private BookingSagaRepository sagaRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private BookingStateChangePersistenceService bookingStateChangePersistenceService;
  @Mock private InventoryReservationPort inventoryReservationPort;
  @Mock private PaymentClient paymentClient;

  private BookingSagaProcessManager processManager;

  @BeforeEach
  void setUp() {
    retryProperties = new BookingSagaRetryProperties();

    BookingSagaBookingLoader bookingLoader = new BookingSagaBookingLoader(bookingRepository);

    BookingSagaActionRegistry actionRegistry =
        new BookingSagaActionRegistry(
            List.of(
                new HoldInventorySagaAction(
                    sagaRepository,
                    bookingLoader,
                    inventoryReservationPort,
                    bookingStateChangePersistenceService),
                new AuthorizePaymentSagaAction(sagaRepository, bookingLoader, paymentClient),
                new ConfirmBookingSagaAction(
                    sagaRepository,
                    bookingLoader,
                    inventoryReservationPort,
                    bookingStateChangePersistenceService),
                new ApprovePaymentSagaAction(sagaRepository, paymentClient),
                new CancelPaymentSagaAction(sagaRepository, paymentClient),
                new ReleaseInventorySagaAction(
                    sagaRepository,
                    bookingLoader,
                    inventoryReservationPort,
                    bookingStateChangePersistenceService),
                new CancelBookingSagaAction(sagaRepository)));

    processManager = new BookingSagaProcessManager(sagaRepository, actionRegistry, retryProperties);

    when(sagaRepository.save(any(BookingSaga.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void shouldCompleteSagaWhenInventoryAndPaymentSucceed() {
    Booking booking = newBooking();
    BookingSaga saga = newSaga(booking);

    when(sagaRepository.findById(saga.getId())).thenReturn(Optional.of(saga));
    when(bookingRepository.findById(any(BookingId.class))).thenReturn(Optional.of(booking));
    when(inventoryReservationPort.placeHold(
            HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1))
        .thenReturn(HOLD_ID);
    when(paymentClient.authorize(any(PaymentAuthorizationRequest.class)))
        .thenReturn(authorizedPayment(booking));
    when(paymentClient.approve(PAYMENT_ID)).thenReturn(approvedPayment(booking));

    BookingSaga result = processManager.process(saga.getId());

    assertEquals(BookingSagaStatus.COMPLETED, result.getStatus());
    assertEquals(BookingSagaStep.COMPLETE, result.getCurrentStep());
    assertEquals(PAYMENT_ID, result.getPaymentId());
    assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

    verify(inventoryReservationPort)
        .placeHold(HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1);
    verify(paymentClient).authorize(any(PaymentAuthorizationRequest.class));
    verify(inventoryReservationPort).confirmHold(HOLD_ID);
    verify(paymentClient).approve(PAYMENT_ID);
    verify(inventoryReservationPort, never()).releaseHold(any(UUID.class));
    verify(bookingStateChangePersistenceService, times(2)).persist(eq(booking), any());
  }

  @Test
  void shouldCompensateSagaWhenPaymentIsDeclined() {
    Booking booking = newBooking();
    BookingSaga saga = newSaga(booking);

    when(sagaRepository.findById(saga.getId())).thenReturn(Optional.of(saga));
    when(bookingRepository.findById(any(BookingId.class))).thenReturn(Optional.of(booking));
    when(inventoryReservationPort.placeHold(
            HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1))
        .thenReturn(HOLD_ID);
    when(paymentClient.authorize(any(PaymentAuthorizationRequest.class)))
        .thenReturn(declinedPayment(booking));

    BookingSaga result = processManager.process(saga.getId());

    assertEquals(BookingSagaStatus.COMPENSATED, result.getStatus());
    assertEquals(BookingSagaStep.COMPLETE, result.getCurrentStep());
    assertEquals(BookingStatus.CANCELLED, booking.getStatus());

    verify(inventoryReservationPort)
        .placeHold(HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1);
    verify(paymentClient).authorize(any(PaymentAuthorizationRequest.class));
    verify(inventoryReservationPort).releaseHold(HOLD_ID);
    verify(inventoryReservationPort, never()).confirmHold(any(UUID.class));
    verify(paymentClient, never()).approve(any(UUID.class));
    verify(bookingStateChangePersistenceService, times(2)).persist(eq(booking), any());
  }

  @Test
  void shouldScheduleRetryWhenPaymentAuthorizationFailsTechnically() {
    Booking booking = newBooking();
    BookingSaga saga = newSaga(booking);

    when(sagaRepository.findById(saga.getId())).thenReturn(Optional.of(saga));
    when(bookingRepository.findById(any(BookingId.class))).thenReturn(Optional.of(booking));
    when(inventoryReservationPort.placeHold(
            HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1))
        .thenReturn(HOLD_ID);
    when(paymentClient.authorize(any(PaymentAuthorizationRequest.class)))
        .thenThrow(new PaymentClientException("payment-service is unavailable"));

    BookingSaga result = processManager.process(saga.getId());

    assertEquals(BookingSagaStatus.WAITING_RETRY, result.getStatus());
    assertEquals(BookingSagaStep.AUTHORIZE_PAYMENT, result.getCurrentStep());
    assertEquals(1, result.getRetryCount());
    assertNotNull(result.getNextAttemptAt());
    assertEquals(BookingStatus.ON_HOLD, booking.getStatus());

    verify(inventoryReservationPort)
        .placeHold(HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1);
    verify(paymentClient).authorize(any(PaymentAuthorizationRequest.class));
    verify(inventoryReservationPort, never()).confirmHold(any(UUID.class));
    verify(inventoryReservationPort, never()).releaseHold(any(UUID.class));
    verify(paymentClient, never()).approve(any(UUID.class));
    verify(bookingStateChangePersistenceService).persist(eq(booking), any());
  }

  @Test
  void shouldResumeWaitingRetrySagaAndCompleteWhenPaymentServiceRecovers() {
    Booking booking = newBooking();
    BookingSaga saga = newSaga(booking);

    when(sagaRepository.findById(saga.getId())).thenReturn(Optional.of(saga));
    when(bookingRepository.findById(any(BookingId.class))).thenReturn(Optional.of(booking));
    when(inventoryReservationPort.placeHold(
            HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1))
        .thenReturn(HOLD_ID);
    when(paymentClient.authorize(any(PaymentAuthorizationRequest.class)))
        .thenThrow(new PaymentClientException("payment-service is unavailable"))
        .thenReturn(authorizedPayment(booking));
    when(paymentClient.approve(PAYMENT_ID)).thenReturn(approvedPayment(booking));

    BookingSaga waitingRetrySaga = processManager.process(saga.getId());

    assertEquals(BookingSagaStatus.WAITING_RETRY, waitingRetrySaga.getStatus());
    assertEquals(BookingSagaStep.AUTHORIZE_PAYMENT, waitingRetrySaga.getCurrentStep());

    BookingSaga completedSaga = processManager.process(waitingRetrySaga.getId());

    assertEquals(BookingSagaStatus.COMPLETED, completedSaga.getStatus());
    assertEquals(BookingSagaStep.COMPLETE, completedSaga.getCurrentStep());
    assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

    verify(inventoryReservationPort)
        .placeHold(HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD.checkIn(), STAY_PERIOD.checkOut(), 1);
    verify(paymentClient, times(2)).authorize(any(PaymentAuthorizationRequest.class));
    verify(inventoryReservationPort).confirmHold(HOLD_ID);
    verify(paymentClient).approve(PAYMENT_ID);
  }

  private Booking newBooking() {
    return Booking.create(USER_ID, HOTEL_ID, ROOM_TYPE_ID, STAY_PERIOD, GUEST_COUNT);
  }

  private BookingSaga newSaga(Booking booking) {
    return BookingSaga.start(booking.getId().value(), PAYMENT_AMOUNT, PAYMENT_CURRENCY);
  }

  private PaymentResult authorizedPayment(Booking booking) {
    return new PaymentResult(
        PAYMENT_ID,
        booking.getId().value(),
        USER_ID_VALUE,
        PAYMENT_AMOUNT,
        PAYMENT_CURRENCY,
        PaymentStatus.AUTHORIZED,
        "FAKE",
        "fake-payment-authorized",
        null);
  }

  private PaymentResult approvedPayment(Booking booking) {
    return new PaymentResult(
        PAYMENT_ID,
        booking.getId().value(),
        USER_ID_VALUE,
        PAYMENT_AMOUNT,
        PAYMENT_CURRENCY,
        PaymentStatus.APPROVED,
        "FAKE",
        "fake-payment-approved",
        null);
  }

  private PaymentResult declinedPayment(Booking booking) {
    return new PaymentResult(
        PAYMENT_ID,
        booking.getId().value(),
        USER_ID_VALUE,
        PAYMENT_AMOUNT,
        PAYMENT_CURRENCY,
        PaymentStatus.DECLINED,
        "FAKE",
        "fake-payment-declined",
        "payment amount exceeds fake provider decline threshold");
  }
}
