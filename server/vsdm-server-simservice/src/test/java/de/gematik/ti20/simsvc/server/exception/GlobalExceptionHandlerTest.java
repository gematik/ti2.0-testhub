/*-
 * #%L
 * VSDM Server Simservice
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
package de.gematik.ti20.simsvc.server.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void thatInternalServerErrorIsPropagated() {
    final ResponseStatusException ex =
        new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "SERVICE_UNSUPPORTED_MEDIATYPE");
    final ResponseEntity<String> response =
        globalExceptionHandler.handleResponseStatusException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
  }

  @Test
  void thatContentTypeIsSetCorrectly() {
    final ResponseStatusException ex =
        new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "SERVICE_UNSUPPORTED_MEDIATYPE");
    final ResponseEntity<String> response =
        globalExceptionHandler.handleResponseStatusException(ex);
    assertThat(response.getHeaders().getContentType().toString())
        .isEqualTo("application/fhir+json;charset=UTF-8");
  }

  @Test
  void thatUnknownCodeCanBeProcessed() {
    final ResponseStatusException ex = new ResponseStatusException(123, "unknown", null);
    final ResponseEntity<String> response =
        globalExceptionHandler.handleResponseStatusException(ex);
    assertThat(response.getStatusCode().value()).isEqualTo(123);
    assertThat(response.getBody()).contains("SERVICE_INTERNAL_SERVER_ERROR");
  }

  @Test
  void thatZetaErrorCanBeProcess() {
    final ZetaErrorException ex = new ZetaErrorException(ErrorCase.MISSING_HEADER_POPP);
    final ResponseEntity<String> response = globalExceptionHandler.handleZetaErrorException(ex);
    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody())
        .contains(
            "{\"error\": \"MISSING_HEADER_POPP\", \"error_description\": \"Header ZETA-PoPP-Token-Content fehlt.\"}");
  }

  @Test
  void thatVsdmErrorIsMappedWithStatusAndInterpolation() {
    final VsdmErrorException ex =
        new VsdmErrorException(ErrorCase.VSDSERVICE_INVALID_IK, Map.of("ik", "A123456789"));

    final ResponseEntity<String> response = globalExceptionHandler.handleVsdmErrorException(ex);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).contains("VSDSERVICE_INVALID_IK");
    assertThat(response.getBody()).contains("A123456789");
    assertThat(response.getBody()).doesNotContain("[ik]");
  }

  @Test
  void thatVsdmErrorWithoutErrorCaseFallsBackToInternalServerError() {
    final VsdmErrorException ex = new VsdmErrorException(null, Map.of("ik", "A123456789"));

    final ResponseEntity<String> response = globalExceptionHandler.handleVsdmErrorException(ex);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).contains("SERVICE_INTERNAL_SERVER_ERROR");
  }
}
