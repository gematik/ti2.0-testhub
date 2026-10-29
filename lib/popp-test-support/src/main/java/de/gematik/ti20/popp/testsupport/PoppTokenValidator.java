/*-
 * #%L
 * PoPP Test Support
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
package de.gematik.ti20.popp.testsupport;

import static de.gematik.ti20.rbel.fluent.RbelFluentApi.expectRequests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

public final class PoppTokenValidator {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String BLUEPRINT_FOLDER = "/de/gematik/ti20/popp/testsupport/blueprints/";
  private static final String TOKEN_PATH = "/token";

  private PoppTokenValidator() {}

  public static void validateClaimsInPoppToken() {
    final var body = loadBlueprint("poppTokenBodyClaims.json");
    validateClaimsInPoppToken(body, null);
  }

  public static void validateClaimsInPoppToken(
      final String expectedIssuer,
      final String expectedKid,
      final String actorProfessionOidPattern) {
    final var body = loadBlueprint("poppTokenBodyClaims.json");
    body.put("iss", Objects.requireNonNull(expectedIssuer));
    body.put("actorProfessionOid", Objects.requireNonNull(actorProfessionOidPattern));
    validateClaimsInPoppToken(body, expectedKid);
  }

  public static void validateClaimsInPoppToken(
      final String expectedIssuer, final String actorProfessionOidPattern) {
    final var body = loadBlueprint("poppTokenBodyClaims.json");
    body.put("iss", Objects.requireNonNull(expectedIssuer));
    body.put("actorProfessionOid", Objects.requireNonNull(actorProfessionOidPattern));
    validateClaimsInPoppToken(body, null);
  }

  private static void validateClaimsInPoppToken(final ObjectNode body, final String expectedKid) {
    final var responseBlueprint = resolveBlueprint(loadBlueprint("poppTokenResponse.json"));
    final var bodyBlueprint = resolveBlueprint(body);

    final ObjectNode headerNode = loadBlueprint("poppTokenHeaderClaims.json");
    if (expectedKid != null) {
      headerNode.put("kid", expectedKid);
    }
    final var headerBlueprint = resolveBlueprint(headerNode);

    expectRequests(TOKEN_PATH)
        .nextResponse(
            response -> {
              response
                  .hasJsonAtPathEqualTo("$.body", responseBlueprint)
                  .hasJsonAtPathEqualTo("$.body.token.content.body", bodyBlueprint)
                  .hasJsonAtPathEqualTo("$.body.token.content.header", headerBlueprint);
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

  public static void validatePoppTokenForBasicErrorResponse() {
    expectRequests(TOKEN_PATH).nextResponse().hasValueAtPathEqualTo("$.body.status", "ERROR");
  }

  public static void validatePoppTokenForInvalidCaErrorResponse(final String errorMessage) {
    expectRequests(TOKEN_PATH)
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.errorMessage", errorMessage);
  }

  public static void assertPatientData(final String patientId, final String insurerId) {
    expectRequests(TOKEN_PATH)
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.token.content.body.patientId", patientId)
        .hasValueAtPathEqualTo("$.body.token.content.body.insurerId", insurerId);
  }

  public static void assertPractitionerData(final String actorId, final String actorProfessionOid) {
    expectRequests(TOKEN_PATH)
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.token.content.body.actorId", actorId)
        .hasValueAtPathEqualTo("$.body.token.content.body.actorProfessionOid", actorProfessionOid);
  }

  public static void proofMethodInTokenMatchesCommTypeOrThrow(final String commType) {
    final String expectedProofMethod;
    if ("kontaktbehaftet".equals(commType)) {
      expectedProofMethod = "ehc-practitioner-trustedchannel";
    } else if ("kontaktlos".equals(commType)) {
      expectedProofMethod = "ehc-practitioner-cvc-authenticated";
    } else {
      throw new AssertionError(
          "expecting commType to be either 'kontaktbehaftet' or 'kontaktlos' but found "
              + commType);
    }
    expectRequests(TOKEN_PATH)
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.token.content.body.proofMethod", expectedProofMethod);
  }

  private static ObjectNode loadBlueprint(final String filename) {
    final String resource = BLUEPRINT_FOLDER + filename;
    try (final var input = PoppTokenValidator.class.getResourceAsStream(resource)) {
      if (input == null) {
        throw new IllegalStateException("PoPP blueprint not found on classpath: " + resource);
      }
      return JSON.readValue(input, ObjectNode.class);
    } catch (final IOException e) {
      throw new IllegalStateException("Unable to read PoPP blueprint: " + resource, e);
    }
  }

  private static String resolveBlueprint(final ObjectNode blueprint) {
    return TigerGlobalConfiguration.resolvePlaceholders(blueprint.toString());
  }
}
