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
package de.gematik.ti20.simsvc.server.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void thatInternalServerErrorIsPropagated() {
    final ResponseStatusException ex =
        new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "VSDSERVICE_UNSUPPORTED_MEDIATYPE");
    final ResponseEntity<String> response =
        globalExceptionHandler.handleResponseStatusException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void thatUnknownCodeCanBeProcessed() {
    final ResponseStatusException ex = new ResponseStatusException(123, "unknown", null);
    final ResponseEntity<String> response =
        globalExceptionHandler.handleResponseStatusException(ex);
    assertThat(response.getStatusCode().value()).isEqualTo(123);
    assertThat(response.getBody()).contains("VSDSERVICE_INTERNAL_SERVER_ERROR");
  }
}
