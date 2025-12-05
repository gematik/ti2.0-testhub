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
package de.gematik.ti20.simsvc.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.simsvc.client.config.VsdmConfig;
import de.gematik.ti20.simsvc.client.pact.PactConfig;
import de.gematik.zeta.sdk.ZetaSdkClient;
import io.ktor.client.HttpClient;
import io.ktor.client.HttpClientKt;
import io.ktor.client.engine.java.Java;
import java.nio.charset.StandardCharsets;
import kotlin.Unit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
@TestPropertySource(value = "classpath:pactconfig.properties")
@MockServerConfig(hostInterface = "localhost")
@Slf4j
class VsdmClientServicePactTest {

  @Autowired private VsdmConfig vsdmConfig;
  @Autowired private VsdmClientService vsdmClientService;
  @Autowired private PactConfig pactConfig;
  @MockitoBean private ZetaSdkClient zetaSdkClient;
  private SimulatorAttachedCard mockEgkCard;

  @BeforeEach
  void beforeEach() {
    // We set the properties in this way, so that we can use the configuration in the
    // @Pact annotation. Otherwise it does not see the values of the spring configuration bean
    System.setProperty("pact.consumer.name", pactConfig.getPactConsumerName());
    System.setProperty("pact.provider.name", pactConfig.getPactProviderName());
    MDC.put("traceId", "test-trace-12345");

    // We mock the zetaSdkClient.httpClient() method to return a regular httpclient which does NOT
    // add any zetaSpecific headers and does not do any additional authentication with a zeta
    // service.
    //  In this way the test returns the mocked responses and registers the pact into the pact file.
    HttpClient customKtorHttpClient =
        HttpClientKt.HttpClient(Java.INSTANCE, config -> Unit.INSTANCE);
    when(zetaSdkClient.httpClient(any())).thenReturn(customKtorHttpClient);

    mockEgkCard = mock(SimulatorAttachedCard.class);
    when(mockEgkCard.isEgk()).thenReturn(true);
    when(mockEgkCard.getSlotId()).thenReturn(1);
    when(mockEgkCard.getId()).thenReturn("card1");
  }

  @AfterEach
  void afterEach() {
    System.clearProperty("pact.consumer.name");
    System.clearProperty("pact.provider.name");
  }

  @SneakyThrows
  @Pact(provider = "${pact.provider.name}", consumer = "${pact.consumer.name}")
  public RequestResponsePact getPatientBundle(PactDslWithProvider builder) {
    String expectedResponse =
        new String(
            getClass()
                .getResourceAsStream("/expectedResponses/getVSDMBundle_200_0.json")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    return builder
        .given(" a patient with ID 10 exists")
        .uponReceiving("a request to get a patient")
        .path("/vsdservice/v1/vsdmbundle")
        .method("GET")
        .willRespondWith()
        .status(200)
        .matchHeader("Content-Type", "application/json; charset=utf-8")
        .body(expectedResponse)
        .toPact();
  }

  // Verify that on response 200 a not-empty response body is returned
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatientResponseStatusCodeIs200(MockServer mockServer) {
    Integer egkSlotId = 1;
    Integer smcBSlotId = 1;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token123", "etag123", false);

    assertThat(patientBundleAsString.getStatusCodeValue()).isEqualTo(200);
    assertThat(patientBundleAsString.getBody()).isNotEmpty();
  }

  // Verify that on response 200 a valid Bundle FHIR resource is returned
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isBundle(MockServer mockServer) {
    Integer egkSlotId = 2;
    Integer smcBSlotId = 2;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token456", "etag456", false);

    String responseBody = patientBundleAsString.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(responseBody);

    assertThat(rootNode.get("resourceType").asText()).isEqualTo("Bundle");
  }

  // Verify that on response 200 the returned Bundle is a VSDMBundle
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isVSDMBundle(MockServer mockServer) {
    Integer egkSlotId = 3;
    Integer smcBSlotId = 3;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token456", "etag456", false);

    String responseBody = patientBundleAsString.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(responseBody);
    JsonNode entries = rootNode.get("entry");

    assertThat(rootNode.get("meta").get("profile").get(0).asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle");
    assertThat(entries.isArray()).isTrue();

    log.info("Entries: {}", entries);

    assertThat(getResourceFromBundle(entries, "Patient").get("resourceType").asText())
        .isEqualTo("Patient");
    assertThat(getResourceFromBundle(entries, "Composition").get("resourceType").asText())
        .isEqualTo("Composition");
    assertThat(getResourceFromBundle(entries, "Coverage").get("resourceType").asText())
        .isEqualTo("Coverage");
  }

  // Verify that on response 200 the returned VSDMBundle includes a valid VSDMPatient resource
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isValidVSDMPatient(MockServer mockServer) {
    Integer egkSlotId = 4;
    Integer smcBSlotId = 4;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token789", "etag789", false);

    String responseBody = patientBundleAsString.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(responseBody);
    JsonNode entries = rootNode.get("entry");

    JsonNode patientResource = getResourceFromBundle(entries, "Patient");

    assertThat(patientResource.get("meta").get("profile").get(0).asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPatient");
  }

  // Verify that on response 200 the returned VSDMBundle includes a valid VSDMComposition resource
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isValidVSDMComposition(MockServer mockServer) {
    Integer egkSlotId = 5;
    Integer smcBSlotId = 5;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token101", "etag101", false);

    String responseBody = patientBundleAsString.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(responseBody);
    JsonNode entries = rootNode.get("entry");

    JsonNode compositionResource = getResourceFromBundle(entries, "Composition");

    assertThat(compositionResource.get("meta").get("profile").get(0).asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMComposition");
  }

  // Verify that on response 200 the returned VSDMBundle includes a valid Coverage resource
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isValidVSDMCoverage(MockServer mockServer) {
    Integer egkSlotId = 6;
    Integer smcBSlotId = 6;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token202", "etag202", false);

    String responseBody = patientBundleAsString.getBody();
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(responseBody);
    JsonNode entries = rootNode.get("entry");

    JsonNode coverageResource = getResourceFromBundle(entries, "Coverage");

    assertThat(coverageResource.get("meta").get("profile").get(0).asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMCoverage");
  }

  /**
   * Get a resource from a Bundle by its resource type.
   *
   * @param entries
   * @param resourceType
   * @return a Resource or null if not found
   */
  private static @Nullable JsonNode getResourceFromBundle(JsonNode entries, String resourceType) {
    JsonNode resource = null;
    for (JsonNode entry : entries) {
      JsonNode targetResource = entry.get("resource");
      if (targetResource != null
          && resourceType.equals(targetResource.get("resourceType").asText())) {
        resource = targetResource;
        break;
      }
    }
    return resource;
  }

  /**
   * Set the target URL of the VSDM Simulation service.
   *
   * @param url
   */
  void configureVsdmUrl(String url) {
    vsdmConfig.setResourceServerUrl(url);
  }
}
