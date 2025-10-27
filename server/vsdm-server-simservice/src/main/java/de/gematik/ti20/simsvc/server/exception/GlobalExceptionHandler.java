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

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.builder.VsdmOperationOutcomeBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<String> handleResponseStatusException(final ResponseStatusException ex) {

    final ErrorCase errorCase = ErrorCase.getByBdeReference(ex.getReason());
    if (errorCase != null) {
      return ResponseEntity.status(errorCase.getHttpCode()).body(operationOutcome(errorCase));
    } else {
      return ResponseEntity.status(ex.getStatusCode())
          .body(operationOutcome(ErrorCase.VSDSERVICE_INTERNAL_SERVER_ERROR));
    }
  }

  private String operationOutcome(final ErrorCase errorCase) {
    final VsdmOperationOutcome vsdmOperationOutcome =
        VsdmOperationOutcomeBuilder.create()
            .withCode(errorCase.getBdeCode())
            .withText(errorCase.getBdeText())
            .withReference(errorCase.getBdeReference())
            .build();

    final String encodedOperationOutcome =
        FhirCodec.forR4().andDummyValidator().encode(vsdmOperationOutcome, EncodingType.JSON);

    return encodedOperationOutcome;
  }
}
