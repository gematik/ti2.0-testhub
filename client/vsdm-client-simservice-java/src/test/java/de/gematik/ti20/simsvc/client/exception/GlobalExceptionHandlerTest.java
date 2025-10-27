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

import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void testHandleZetaHttpResponseException() {
    ZetaHttpResponseException ex = new ZetaHttpResponseException(418, "Fehler", null);
    ResponseEntity<Map<String, String>> response = handler.handleZetaHttpResponseException(ex);

    assertEquals(418, response.getStatusCodeValue());
    assertEquals("Zeta HTTP Response error", response.getBody().get("error"));
    assertEquals("Fehler", response.getBody().get("message"));
  }

  @Test
  void testHandleZetaHttpException() {
    ZetaHttpException ex = new ZetaHttpException("Gateway down", null);
    ResponseEntity<Map<String, String>> response = handler.handleZetaHttpException(ex);

    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    assertEquals("Zeta HTTP error", response.getBody().get("error"));
    assertEquals("Gateway down", response.getBody().get("message"));
  }

  @Test
  void testHandleGenericException() {
    Exception ex = new Exception("Unerwartet");
    ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Internal Server Error", response.getBody().get("error"));
    assertTrue(response.getBody().get("message").contains("Unerwartet"));
  }
}
