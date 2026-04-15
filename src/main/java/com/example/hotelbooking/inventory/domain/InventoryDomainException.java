package com.example.hotelbooking.inventory.domain;

import java.io.Serial;

public class InventoryDomainException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public InventoryDomainException(String message) {
    super(message);
  }
}
