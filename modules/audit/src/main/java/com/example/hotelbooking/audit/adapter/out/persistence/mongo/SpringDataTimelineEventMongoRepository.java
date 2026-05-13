package com.example.hotelbooking.audit.adapter.out.persistence.mongo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataTimelineEventMongoRepository
    extends MongoRepository<TimelineEventDocument, String> {

  List<TimelineEventDocument> findByBookingIdOrderByOccurredAtAscRecordedAtAsc(UUID bookingId);
}
