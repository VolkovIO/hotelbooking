package com.example.hotelbooking.audit.application.service;

import com.example.hotelbooking.audit.application.port.out.TimelineEventRepository;
import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingTimelineQueryService {

  private final TimelineEventRepository timelineEventRepository;

  public List<TimelineEvent> findByBookingId(UUID bookingId) {
    return timelineEventRepository.findByBookingId(bookingId);
  }
}
