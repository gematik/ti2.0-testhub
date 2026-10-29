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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.test.tiger.common.config.ConfigurationValuePrecedence;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.rbel.ModeType;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.ti20.rbel.fluent.RbelFluentApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class PoppTokenValidatorTest {
  private final RbelMessageRetriever retriever = mock(RbelMessageRetriever.class);
  private final RbelValidator validator = mock(RbelValidator.class);
  private final RbelElement response = mock(RbelElement.class);

  @BeforeEach
  void setUp() {
    when(retriever.getCurrentRequest()).thenReturn(mock(RbelElement.class));
    when(retriever.getCurrentResponse()).thenReturn(response);
    RbelFluentApi.useDefaultComponentsForTesting(
        RbelFluentApi.RbelComponents.of(retriever, validator));
    TigerGlobalConfiguration.putValue(
        "popp.server.kidTokenKey", "test-key", ConfigurationValuePrecedence.TEST_CONTEXT);
    TigerGlobalConfiguration.putValue(
        "popp.server.url", "https://popp.example", ConfigurationValuePrecedence.TEST_CONTEXT);
  }

  @AfterEach
  void tearDown() {
    RbelFluentApi.clearDefaultComponentsForTesting();
  }

  @Test
  void validatesPackagedBlueprintsWithPoppDefaults() throws IOException {
    givenValidTimestamps();

    PoppTokenValidator.validateClaimsInPoppToken();

    final var blueprints = capturedBlueprints();
    assertEquals("OK", blueprints.get("$.body").get("status").asText());
    assertEquals("ey.*", blueprints.get("$.body").get("token").asText());
    final var body = blueprints.get("$.body.token.content.body");
    assertEquals("1.0.0", body.get("version").asText());
    assertEquals("${json-unit.ignore}", body.get("iss").asText());
    assertEquals("${json-unit.ignore}", body.get("iat").asText());
    assertEquals("${json-unit.ignore}", body.get("patientProofTime").asText());
    assertTrue(body.get("actorProfessionOid").asText().contains("1.2.276.0.76.4.245"));
    final var header = blueprints.get("$.body.token.content.header");
    assertEquals("test-key", header.get("kid").asText());
    assertEquals("ES256", header.get("alg").asText());
    assertEquals("vnd.telematik.popp+jwt", header.get("typ").asText());
    verifyNoTokenStorage();
  }

  @Test
  void supportsStricterIssuerAndProfessionExpectations() throws IOException {
    givenValidTimestamps();
    final var professionPattern =
        "(1.2.276.0.76.4.50)|(1.2.276.0.76.4.51)|(1.2.276.0.76.4.52)|(1.2.276.0.76.4.53)|(1.2.276.0.76.4.54)";

    PoppTokenValidator.validateClaimsInPoppToken("${popp.server.url}", professionPattern);

    final var body = capturedBlueprints().get("$.body.token.content.body");
    assertEquals("https://popp.example", body.get("iss").asText());
    assertEquals(professionPattern, body.get("actorProfessionOid").asText());
    assertFalse(body.get("actorProfessionOid").asText().contains("1.2.276.0.76.4.245"));
  }

  @Test
  void supportsCustomIssuerProfessionAndKeyExpectations() throws IOException {
    givenValidTimestamps();
    final var professionPattern = "1.2.276.0.76.4.50";

    PoppTokenValidator.validateClaimsInPoppToken(
        "${popp.server.url}", "custom-key", professionPattern);

    final var blueprints = capturedBlueprints();
    final var body = blueprints.get("$.body.token.content.body");
    assertEquals("https://popp.example", body.get("iss").asText());
    assertEquals(professionPattern, body.get("actorProfessionOid").asText());
    assertEquals("custom-key", blueprints.get("$.body.token.content.header").get("kid").asText());
  }

  @Test
  void rejectsNullIssuerAndProfessionExpectations() {
    assertThrows(
        NullPointerException.class,
        () -> PoppTokenValidator.validateClaimsInPoppToken(null, "kid", "profession"));
    assertThrows(
        NullPointerException.class,
        () -> PoppTokenValidator.validateClaimsInPoppToken("issuer", "kid", null));
    assertThrows(
        NullPointerException.class,
        () -> PoppTokenValidator.validateClaimsInPoppToken(null, "profession"));
    assertThrows(
        NullPointerException.class,
        () -> PoppTokenValidator.validateClaimsInPoppToken("issuer", null));
    verifyNoInteractions(validator);
  }

  @ParameterizedTest
  @ValueSource(strings = {"iat", "patientProofTime"})
  void rejectsTimestampsBeyondThirtySecondsInTheFuture(final String claim) {
    givenValidTimestamps();
    givenValue(
        "$.body.token.content.body." + claim,
        Long.toString(Instant.now().plusSeconds(60).getEpochSecond()));

    assertThrows(AssertionError.class, PoppTokenValidator::validateClaimsInPoppToken);
  }

  @Test
  void acceptsTimestampsWithinThirtySecondsInTheFuture() {
    final var timestamp = Long.toString(Instant.now().plusSeconds(29).getEpochSecond());
    givenValue("$.body.token.content.body.iat", timestamp);
    givenValue("$.body.token.content.body.patientProofTime", timestamp);

    PoppTokenValidator.validateClaimsInPoppToken();
  }

  @Test
  void propagatesBlueprintValidationFailures() {
    final var failure = new AssertionError("Invalid token response");
    doThrow(failure)
        .when(validator)
        .assertAttributeOfCurrentResponseMatchesAs(
            eq("$.body"), eq(ModeType.JSON), anyString(), eq(""), eq(retriever));

    assertSame(
        failure, assertThrows(AssertionError.class, PoppTokenValidator::validateClaimsInPoppToken));
  }

  @Test
  void validatesPatientAndPractitionerDataWithoutSuiteModels() {
    givenValue("$.body.token.content.body.patientId", "patient");
    givenValue("$.body.token.content.body.insurerId", "insurer");
    givenValue("$.body.token.content.body.actorId", "actor");
    givenValue("$.body.token.content.body.actorProfessionOid", "1.2.276.0.76.4.50");

    PoppTokenValidator.assertPatientData("patient", "insurer");
    PoppTokenValidator.assertPractitionerData("actor", "1.2.276.0.76.4.50");
  }

  @Test
  void rejectsMismatchingPatientAndPractitionerData() {
    givenValue("$.body.token.content.body.patientId", "wrong");
    givenValue("$.body.token.content.body.actorId", "wrong");

    assertThrows(
        AssertionError.class, () -> PoppTokenValidator.assertPatientData("patient", "insurer"));
    assertThrows(
        AssertionError.class, () -> PoppTokenValidator.assertPractitionerData("actor", "oid"));
  }

  @ParameterizedTest
  @CsvSource({
    "kontaktbehaftet, ehc-practitioner-trustedchannel",
    "kontaktlos, ehc-practitioner-cvc-authenticated"
  })
  void mapsCommunicationTypesToProofMethods(final String commType, final String proofMethod) {
    givenValue("$.body.token.content.body.proofMethod", proofMethod);

    PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow(commType);
  }

  @Test
  void rejectsMismatchingProofMethods() {
    givenValue("$.body.token.content.body.proofMethod", "healthid");

    assertThrows(
        AssertionError.class,
        () -> PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow("kontaktlos"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "unsupported"})
  void rejectsUnknownCommunicationTypes(final String commType) {
    assertThrows(
        AssertionError.class,
        () -> PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow(commType));
    verifyNoInteractions(validator);
  }

  @Test
  void validatesErrorResponses() {
    givenValue("$.body.status", "ERROR");
    givenValue("$.body.errorMessage", "Invalid CA");

    PoppTokenValidator.validatePoppTokenForBasicErrorResponse();
    PoppTokenValidator.validatePoppTokenForInvalidCaErrorResponse("Invalid CA");
  }

  @Test
  void rejectsUnexpectedErrorResponses() {
    givenValue("$.body.status", "OK");
    givenValue("$.body.errorMessage", "Unexpected");

    assertThrows(AssertionError.class, PoppTokenValidator::validatePoppTokenForBasicErrorResponse);
    assertThrows(
        AssertionError.class,
        () -> PoppTokenValidator.validatePoppTokenForInvalidCaErrorResponse("Invalid CA"));
  }

  private void givenValidTimestamps() {
    final var timestamp = Long.toString(Instant.now().minusSeconds(5).getEpochSecond());
    givenValue("$.body.token.content.body.iat", timestamp);
    givenValue("$.body.token.content.body.patientProofTime", timestamp);
  }

  private void givenValue(final String path, final String value) {
    final var element = new RbelElement(value.getBytes(StandardCharsets.UTF_8), null);
    when(response.findRbelPathMembers(path)).thenReturn(List.of(element));
    when(retriever.findElementInCurrentResponse(path)).thenReturn(element);
  }

  private Map<String, JsonNode> capturedBlueprints() throws IOException {
    final var paths = ArgumentCaptor.forClass(String.class);
    final var json = ArgumentCaptor.forClass(String.class);
    verify(validator, times(3))
        .assertAttributeOfCurrentResponseMatchesAs(
            paths.capture(), eq(ModeType.JSON), json.capture(), eq(""), eq(retriever));
    final Map<String, JsonNode> result = new HashMap<>();
    for (int i = 0; i < paths.getAllValues().size(); i++) {
      result.put(
          paths.getAllValues().get(i), new ObjectMapper().readTree(json.getAllValues().get(i)));
    }
    return result;
  }

  private void verifyNoTokenStorage() {
    verify(retriever, times(0)).findElementInCurrentResponse("$.body.token");
  }
}
