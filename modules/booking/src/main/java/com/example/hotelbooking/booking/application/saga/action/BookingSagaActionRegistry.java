package com.example.hotelbooking.booking.application.saga.action;

import com.example.hotelbooking.booking.application.saga.BookingSaga;
import com.example.hotelbooking.booking.application.saga.BookingSagaStateException;
import com.example.hotelbooking.booking.application.saga.BookingSagaStep;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BookingSagaActionRegistry {

  private final Map<BookingSagaStep, BookingSagaAction> actions;

  public BookingSagaActionRegistry(List<BookingSagaAction> actionList) {
    this.actions = actionsByStep(actionList);
  }

  public BookingSaga execute(BookingSaga saga) {
    if (saga.getCurrentStep() == BookingSagaStep.COMPLETE) {
      return saga;
    }

    BookingSagaAction action = actions.get(saga.getCurrentStep());

    if (action == null) {
      throw new BookingSagaStateException(
          "No booking saga action found for step " + saga.getCurrentStep());
    }

    return action.execute(saga);
  }

  private Map<BookingSagaStep, BookingSagaAction> actionsByStep(
      List<BookingSagaAction> actionList) {
    Map<BookingSagaStep, BookingSagaAction> result = new EnumMap<>(BookingSagaStep.class);

    for (BookingSagaAction action : actionList) {
      result.put(action.step(), action);
    }

    return Map.copyOf(result);
  }
}
