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

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.builder.VsdmOperationOutcomeBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
      return ResponseEntity.status(errorCase.getHttpCode())
          .contentType(new MediaType("application", "fhir+json", StandardCharsets.UTF_8))
          .body(operationOutcome(errorCase, Map.of()));
    } else {
      return ResponseEntity.status(ex.getStatusCode())
          .contentType(new MediaType("application", "fhir+json", StandardCharsets.UTF_8))
          .body(operationOutcome(ErrorCase.SERVICE_INTERNAL_SERVER_ERROR, Map.of()));
    }
  }

  @ExceptionHandler(VsdmErrorException.class)
  public ResponseEntity<String> handleVsdmErrorException(final VsdmErrorException ex) {
    final ErrorCase errorCase = ex.getErrorCase();
    if (errorCase != null) {
      return ResponseEntity.status(errorCase.getHttpCode())
          .contentType(new MediaType("application", "fhir+json", StandardCharsets.UTF_8))
          .body(operationOutcome(errorCase, ex.getValues()));
    } else {
      return ResponseEntity.status(500)
          .contentType(new MediaType("application", "fhir+json", StandardCharsets.UTF_8))
          .body(operationOutcome(ErrorCase.SERVICE_INTERNAL_SERVER_ERROR, ex.getValues()));
    }
  }

  @ExceptionHandler(ZetaErrorException.class)
  public ResponseEntity<String> handleZetaErrorException(final ZetaErrorException ex) {
    final ErrorCase errorCase = ex.getErrorCase();
    final String zetaError =
        "{\"error\": \""
            + errorCase.getBdeReference()
            + "\", \"error_description\": \""
            + errorCase.getBdeText()
            + "\"}";
    return ResponseEntity.status(errorCase.getHttpCode())
        .header("ZETA-Cause", "Proxy")
        .body(zetaError);
  }

  private String operationOutcome(final ErrorCase errorCase, final Map<String, String> values) {
    final VsdmOperationOutcome vsdmOperationOutcome =
        VsdmOperationOutcomeBuilder.create()
            .withCode(errorCase.getBdeCode())
            .withText(interpolate(errorCase.getBdeText(), values))
            .withReference(errorCase.getBdeReference())
            .build();

    return FhirCodec.forR4().andDummyValidator().encode(vsdmOperationOutcome, EncodingType.JSON);
  }

  private static final Pattern PLACEHOLDER = Pattern.compile("\\[([^\\[\\]]+)]");

  private String interpolate(final String template, final Map<String, String> values) {
    if (template == null || template.isEmpty() || values == null || values.isEmpty()) {
      return template;
    }

    final Matcher matcher = PLACEHOLDER.matcher(template);
    final StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      final String key = matcher.group(1); // Inhalt zwischen [ und ]
      final String replacement = values.get(key);

      if (replacement == null) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
      } else {
        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
      }
    }

    matcher.appendTail(result);
    return result.toString();
  }
}
