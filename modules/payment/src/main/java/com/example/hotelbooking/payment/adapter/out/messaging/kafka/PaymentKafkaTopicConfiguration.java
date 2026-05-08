package com.example.hotelbooking.payment.adapter.out.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("payment-outbox-kafka")
class PaymentKafkaTopicConfiguration {

  @Bean
  NewTopic paymentEventsTopic(
      @Value("${app.payment.kafka.payment-events-topic:payment.events}") String topic) {
    return TopicBuilder.name(topic).partitions(3).replicas(1).build();
  }
}
