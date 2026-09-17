/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.exception;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Global exception handler for the Card Terminal Simulator. */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String ERROR_KEY = "error";
  private static final String MESSAGE_KEY = "message";

  /** Handle IllegalArgumentException, particularly for deprecated algorithms. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
      final IllegalArgumentException e) {
    logger.warn("Invalid argument: {}", e.getMessage());

    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put(ERROR_KEY, "Bad Request");
    errorResponse.put(MESSAGE_KEY, e.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /** Handle ResponseStatusException from controllers. */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatusException(
      final ResponseStatusException e) {
    logger.warn("Response status exception: {}", e.getReason());

    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put(ERROR_KEY, e.getStatusCode().toString());
    errorResponse.put(MESSAGE_KEY, e.getReason());

    return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(CardNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleCardNotFoundException(
      final CardNotFoundException e) {
    logger.info("CardNotFound exception: ", e);

    final Map<String, String> errorInfo = new HashMap<>();
    errorInfo.put(ERROR_KEY, "Card not found");
    errorInfo.put(MESSAGE_KEY, "No card found for handle: " + e.getCardId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorInfo);
  }

  /** Handle generic exceptions as fallback. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGenericException(final Exception e) {
    logger.error("Unexpected error: ", e);

    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put(ERROR_KEY, "Internal Server Error");
    errorResponse.put(MESSAGE_KEY, "An unexpected error occurred: " + e.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
