package com.example.hotelbooking.audit.application.port.out;

import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.util.List;
import java.util.UUID;

public interface TimelineEventRepository {

  boolean saveIfAbsent(TimelineEvent event);

  List<TimelineEvent> findByBookingId(UUID bookingId);
}
