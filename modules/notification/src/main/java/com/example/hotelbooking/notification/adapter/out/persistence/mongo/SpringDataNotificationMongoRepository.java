package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataNotificationMongoRepository
    extends MongoRepository<NotificationDocument, String> {

  List<NotificationDocument> findByUserIdOrderByCreatedAtDesc(String userId);
}
