package com.example.hotelbooking.inventory.adapter.out.persistence.mongo;

import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepository;
import com.example.hotelbooking.inventory.application.port.out.RoomAvailabilityRepositoryContractTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SuppressWarnings(
    "PMD.TestClassWithoutTestCases") // Test cases are inherited from repository contract.
@DataMongoTest
@Testcontainers
@ActiveProfiles("mongo")
@Import(MongoRoomAvailabilityRepositoryAdapter.class)
class MongoRoomAvailabilityRepositoryAdapterIntegrationTest
    implements RoomAvailabilityRepositoryContractTest {

  @Container @ServiceConnection static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

  @Autowired private SpringDataMongoRoomAvailabilityRepository springDataRepository;

  @Autowired private MongoRoomAvailabilityRepositoryAdapter adapter;

  @BeforeEach
  void cleanUp() {
    springDataRepository.deleteAll();
  }

  @Override
  public RoomAvailabilityRepository repository() {
    return adapter;
  }
}
