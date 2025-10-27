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
package de.gematik.ti20.simsvc.client.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void testHandleIllegalArgumentException() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
    ResponseEntity<Map<String, String>> response = handler.handleIllegalArgumentException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Bad Request", response.getBody().get("error"));
    assertEquals("Invalid input", response.getBody().get("message"));
  }

  @Test
  void testHandleResponseStatusException() {
    ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    ResponseEntity<Map<String, String>> response = handler.handleResponseStatusException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("404 NOT_FOUND", response.getBody().get("error"));
    assertEquals("Not found", response.getBody().get("message"));
  }

  @Test
  void testHandleGenericException() {
    Exception ex = new Exception("Something went wrong");
    ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Internal Server Error", response.getBody().get("error"));
    assertTrue(response.getBody().get("message").contains("Something went wrong"));
  }
}
