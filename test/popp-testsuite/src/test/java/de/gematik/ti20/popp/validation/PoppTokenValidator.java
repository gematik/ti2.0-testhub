/*-
 * #%L
 * PoPP Testsuite
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
package de.gematik.ti20.popp.validation;

import static de.gematik.ti20.popp.data.TestConstants.*;
import static de.gematik.ti20.rbel.fluent.RbelFluentApi.expectRequests;

import de.gematik.ti20.popp.CommunicationType;
import de.gematik.ti20.popp.EgkType;
import de.gematik.ti20.popp.SmcbType;
import java.time.Instant;

public final class PoppTokenValidator {
  private PoppTokenValidator() {}

  public static void validateClaimsInPoppToken() {
    expectRequests(".*/token")
        .nextResponse(
            response -> {
              response
                  .hasJsonAtPathEqualToFile("$.body", VALID_POPP_TOKEN_JSON_RESPONSE_FILE)
                  .hasJsonAtPathEqualToFile(
                      "$.body.token.content.body", VALID_POPP_TOKEN_BODY_CLAIMS_FILE)
                  .hasJsonAtPathEqualToFile(
                      "$.body.token.content.header", VALID_POPP_TOKEN_HEADER_CLAIMS_FILE);

              response
                  .childAtPath("$.body.token.content.body.iat")
                  .mapToInstant()
                  .isBefore(Instant.now().plusSeconds(30));
              response
                  .childAtPath("$.body.token.content.body.patientProofTime")
                  .mapToInstant()
                  .isBefore(Instant.now().plusSeconds(30));
            });
  }

  public static void validatePoppTokenforBasicErrorResponse() {
    expectRequests(".*/token").nextResponse().hasValueAtPathEqualTo("$.body.status", "ERROR");
  }

  public static void validatePoppTokenforInvalidCaErrorResponse(final String errorMessage) {
    expectRequests(".*/token")
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.errorMessage", errorMessage);
  }

  public static void assertThatPatientDataInTokenMatchesDataOnEgk(final EgkType egkType) {
    expectRequests(".*/token")
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.token.content.body.patientId", egkType.getKvnr())
        .hasValueAtPathEqualTo("$.body.token.content.body.insurerId", egkType.getIkNumber());
  }

  public static void asserThatPractitionerDataInTokenMatchesDataOnSmcb(final SmcbType smcbType) {
    expectRequests(".*/token")
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.token.content.body.actorId", smcbType.getTelematikId())
        .hasValueAtPathEqualTo(
            "$.body.token.content.body.actorProfessionOid", smcbType.getProfessionOid());
  }

  public static void proofMethodInTokenMatchesCommTypeOrThrow(final CommunicationType commType) {
    final String expectedProofMethod;
    if (commType.getCommType().equals("kontaktbehaftet")) {
      expectedProofMethod = "ehc-practitioner-trustedchannel";
    } else if (commType.getCommType().equals("kontaktlos")) {
      expectedProofMethod = "ehc-practitioner-cvc-authenticated";
    } else {
      throw new AssertionError(
          "expecting commType to be either 'kontaktbehaftet' or 'kontaktlos' but found "
              + commType.getCommType());
    }
    expectRequests(".*/token")
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.token.content.body.proofMethod", expectedProofMethod);
  }
}
