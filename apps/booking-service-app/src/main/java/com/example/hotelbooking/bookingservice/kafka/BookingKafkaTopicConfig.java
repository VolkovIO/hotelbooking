package com.example.hotelbooking.bookingservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("outbox-kafka")
class BookingKafkaTopicConfig {

  @Bean
  NewTopic bookingEventsTopic(
      @Value("${app.booking.outbox.kafka.topic-name:booking.events}") String topicName,
      @Value("${app.booking.outbox.kafka.partitions:3}") int partitions,
      @Value("${app.booking.outbox.kafka.replicas:1}") int replicas) {
    return TopicBuilder.name(topicName).partitions(partitions).replicas(replicas).build();
  }
}
