package com.example.hotelbooking.notification.adapter.out.persistence.mongo;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("notification-mongo")
@EnableMongoRepositories(basePackageClasses = SpringDataNotificationMongoRepository.class)
class NotificationMongoPersistenceConfiguration {}
