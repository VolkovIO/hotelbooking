package com.example.hotelbooking.observability.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

/**
 * Colors local console log lines depending on the log level.
 *
 * <p>This converter is intentionally designed for local development and demo runs. Production logs
 * should normally avoid ANSI escape sequences because centralized log systems apply colors in their
 * own UI and usually prefer plain text or structured JSON.
 *
 * <p>Color mapping:
 *
 * <ul>
 *   <li>ERROR - red, because it requires immediate attention.
 *   <li>WARN - yellow, because it is abnormal but not necessarily fatal.
 *   <li>INFO - default terminal color, because it is the most frequent level and should not
 *       dominate the screen.
 *   <li>DEBUG - magenta, because it should be visually different from normal informational flow.
 *   <li>TRACE - white, because the project almost never uses it and it should stay neutral.
 * </ul>
 */
public final class HotelbookingLogLevelColorConverter
    extends ForegroundCompositeConverterBase<ILoggingEvent> {

  @Override
  protected String getForegroundColorCode(ILoggingEvent event) {
    Level level = event.getLevel();

    if (Level.ERROR.equals(level)) {
      return ANSIConstants.RED_FG;
    }
    if (Level.WARN.equals(level)) {
      return ANSIConstants.YELLOW_FG;
    }
    if (Level.DEBUG.equals(level)) {
      return ANSIConstants.MAGENTA_FG;
    }
    if (Level.TRACE.equals(level)) {
      return ANSIConstants.WHITE_FG;
    }

    return ANSIConstants.DEFAULT_FG;
  }
}
