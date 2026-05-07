package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

interface SpringDataNotificationMongoRepository
    extends MongoRepository<NotificationDocument, String> {}
