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
package de.gematik.ti20.simsvc.server.controller;

import de.gematik.ti20.simsvc.server.model.SecurityParams;
import de.gematik.ti20.simsvc.server.model.TokenGenerationParams;
import de.gematik.ti20.simsvc.server.model.TokenParams;
import de.gematik.ti20.simsvc.server.service.PoppTokenService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/popp/test/api/v1")
public class PoppTestControllerV1 {

  private final PoppTokenService poppTokenService;

  public PoppTestControllerV1(final PoppTokenService poppTokenService) {
    this.poppTokenService = poppTokenService;
  }

  private static final List<String> PROOF_METHODS =
      List.of(
          "healthid",
          "ehc-practitioner-trustedchannel",
          "ehc-practitioner-cvc-authenticated",
          "ehc-practitioner-user-x509",
          "ehc-practitioner-owner-x509",
          "ehc-provider-trustedchannel",
          "ehc-provider-cvc-authenticated",
          "ehc-provider-user-x509",
          "ehc-provider-owner-x509");

  @PostMapping("/token-generator")
  @Operation(summary = "Generates mock POPP tokens")
  public ResponseEntity<?> generate(
      @RequestBody final TokenGenerationParams tokenGenerationParams) {
    log.debug("Received request to generate tokens.");

    final List<TokenParams> tokenParamsList = tokenGenerationParams.getTokenParamsList();

    // Extract tokenArgs from the request body
    if (tokenParamsList.isEmpty()) {
      log.error("Invalid request: tokenParams is missing or empty.");
      return ResponseEntity.badRequest().body(Map.of("error", "tokenParams is missing or empty."));
    }

    final List<String> tokenResults = new ArrayList<>();

    // Generate token for each set of arguments
    for (final TokenParams tokenParams : tokenParamsList) {
      try {

        if (tokenParams.getPatientId() == null
            || tokenParams.getInsurerId() == null
            || tokenParams.getActorId() == null
            || tokenParams.getActorProfessionOid() == null) {
          log.error("Invalid arguments: One or more required fields are missing.");
          return ResponseEntity.badRequest()
              .body(Map.of("error", "One or more required fields are missing."));
        }

        if (!PROOF_METHODS.contains(tokenParams.getProofMethod())) {
          return ResponseEntity.badRequest()
              .body(Map.of("error", "Unknown proofMethod: " + tokenParams.getProofMethod()));
        }

        final SecurityParams securityParams = tokenGenerationParams.getSecurityParams();
        final String token = poppTokenService.createToken(tokenParams, securityParams);

        tokenResults.add(token);
      } catch (final Exception e) {
        log.error("Error generating token: {}", e.getMessage(), e);
        return ResponseEntity.status(500)
            .body(Map.of("error", "Internal server error while generating tokens."));
      }
    }

    log.debug("Successfully generated {} tokens.", tokenResults.size());
    return ResponseEntity.ok(Map.of("tokenResults", tokenResults));
  }
}
