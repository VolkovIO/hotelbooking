package com.example.hotelbooking.audit.adapter.out.persistence.mongo;

import com.example.hotelbooking.audit.application.port.out.TimelineEventRepository;
import com.example.hotelbooking.audit.domain.TimelineEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@Profile("audit-mongo")
@RequiredArgsConstructor
class MongoTimelineEventRepository implements TimelineEventRepository {

  private final SpringDataTimelineEventMongoRepository repository;

  @Override
  public boolean saveIfAbsent(TimelineEvent event) {
    TimelineEventDocument document = TimelineEventDocument.fromDomain(event);

    try {
      repository.insert(document);
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }

  @Override
  public List<TimelineEvent> findByBookingId(UUID bookingId) {
    return repository.findByBookingIdOrderByOccurredAtAscRecordedAtAsc(bookingId).stream()
        .map(TimelineEventDocument::toDomain)
        .toList();
  }
}
