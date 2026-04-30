package com.example.hotelbooking.inventory.application.exception;

import java.io.Serial;

public abstract class InventoryApplicationException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  protected InventoryApplicationException(String message) {
    super(message);
  }

  protected InventoryApplicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
