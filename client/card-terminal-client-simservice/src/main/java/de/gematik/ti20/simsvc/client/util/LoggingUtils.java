/*
 *
 * Copyright 2025 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.ti20.simsvc.client.util;

import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Utility class for structured logging. This class provides methods for creating consistent log
 * entries with context.
 */
public class LoggingUtils {

  /**
   * Log with context information. This method adds MDC context to logs and then removes it after
   * logging.
   *
   * @param logger The logger to use
   * @param level The log level (debug, info, warn, error)
   * @param message The log message
   * @param context A map of context values to add to MDC
   */
  public static void logWithContext(
      Logger logger, LogLevel level, String message, Map<String, String> context) {
    // Add all context to MDC
    if (context != null) {
      context.forEach(MDC::put);
    }

    try {
      // Log the message
      switch (level) {
        case DEBUG:
          logger.debug(message);
          break;
        case INFO:
          logger.info(message);
          break;
        case WARN:
          logger.warn(message);
          break;
        case ERROR:
          logger.error(message);
          break;
      }
    } finally {
      // Clean up MDC
      if (context != null) {
        context.keySet().forEach(MDC::remove);
      }
    }
  }

  /**
   * Execute a function with context logging. This method adds MDC context before execution and
   * cleans it up afterward.
   *
   * @param <T> The return type of the function
   * @param logger The logger to use
   * @param level The log level for start/end messages
   * @param operation The name of the operation for logging
   * @param context A map of context values to add to MDC
   * @param supplier The function to execute
   * @return The result of the function
   */
  public static <T> T executeWithLogging(
      Logger logger,
      LogLevel level,
      String operation,
      Map<String, String> context,
      Supplier<T> supplier) {
    long startTime = System.currentTimeMillis();

    // Add all context to MDC
    if (context != null) {
      context.forEach(MDC::put);
    }

    try {
      // Log operation start
      switch (level) {
        case DEBUG:
          logger.debug("Starting operation: {}", operation);
          break;
        case INFO:
          logger.info("Starting operation: {}", operation);
          break;
        case WARN:
          logger.warn("Starting operation: {}", operation);
          break;
        case ERROR:
          logger.error("Starting operation: {}", operation);
          break;
      }

      // Execute the operation
      return supplier.get();
    } finally {
      // Log operation end with duration
      long duration = System.currentTimeMillis() - startTime;
      switch (level) {
        case DEBUG:
          logger.debug("Completed operation: {} in {} ms", operation, duration);
          break;
        case INFO:
          logger.info("Completed operation: {} in {} ms", operation, duration);
          break;
        case WARN:
          logger.warn("Completed operation: {} in {} ms", operation, duration);
          break;
        case ERROR:
          logger.error("Completed operation: {} in {} ms", operation, duration);
          break;
      }

      // Clean up MDC
      if (context != null) {
        context.keySet().forEach(MDC::remove);
      }
    }
  }

  /** Enum for log levels. */
  public enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
  }
}
