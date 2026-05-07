package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataNotificationPreferenceMongoRepository
    extends MongoRepository<NotificationPreferenceDocument, String> {}
