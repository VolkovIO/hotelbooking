package com.example.hotelbooking.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.example.hotelbooking.inventory")
@EnableMongoRepositories(
    basePackages = "com.example.hotelbooking.inventory.adapter.out.persistence.mongo")
public class InventoryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InventoryServiceApplication.class, args);
  }
}
