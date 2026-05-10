package com.example.hotelbooking.booking.application.saga;

import com.example.hotelbooking.booking.application.port.out.BookingSagaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Continues booking sagas that were paused after a retryable technical failure.
 *
 * <p>The scheduler intentionally does not own business logic. It only finds sagas ready for retry
 * and delegates execution back to BookingSagaProcessManager.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaRetryScheduler {

  private final BookingSagaRepository sagaRepository;
  private final BookingSagaProcessManager processManager;
  private final BookingSagaRetryProperties properties;

  @Scheduled(fixedDelayString = "${app.booking.saga.retry.scheduler-fixed-delay-ms:5000}")
  public void processReadySagas() {
    if (!properties.isEnabled()) {
      return;
    }

    List<BookingSaga> sagas =
        sagaRepository.findReadyForRetry(Instant.now(), properties.getBatchSize());

    if (sagas.isEmpty()) {
      log.trace("No booking sagas ready for retry");
      return;
    }

    log.info("Found booking sagas ready for retry: count={}", sagas.size());

    for (BookingSaga saga : sagas) {
      processSaga(saga);
    }
  }

  private void processSaga(BookingSaga saga) {
    processManager.process(saga.getId());
  }
}
