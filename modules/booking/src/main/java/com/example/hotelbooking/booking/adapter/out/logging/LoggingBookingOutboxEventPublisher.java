package com.example.hotelbooking.booking.adapter.out.logging;

import com.example.hotelbooking.booking.application.event.BookingOutboxMessage;
import com.example.hotelbooking.booking.application.port.out.BookingOutboxEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("outbox-logging")
class LoggingBookingOutboxEventPublisher implements BookingOutboxEventPublisher {

  @Override
  public void publish(BookingOutboxMessage message) {
    // This adapter will be replaced by Kafka publication in v0.7.0.
    log.info(
        "Publishing booking outbox event through logging adapter: "
            + "eventId={}, eventType={}, eventVersion={}, aggregateType={}, aggregateId={}, payload={}",
        message.id(),
        message.eventType(),
        message.eventVersion(),
        message.aggregateType(),
        message.aggregateId(),
        message.payload());
  }
}
