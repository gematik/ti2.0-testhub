/*-
 * #%L
 * VSDM Client Simulator Service
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
package de.gematik.ti20.simsvc.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import de.gematik.ti20.simsvc.client.pact.PactConfig;
import de.gematik.zeta.sdk.ZetaSdkClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("local")
@ExtendWith(PactConsumerTestExt.class)
@TestPropertySource(value = "classpath:pactconfig.properties")
@MockServerConfig(hostInterface = "localhost")
@Slf4j
class VsdmClientServicePactTest {

  @Autowired private VsdmClientService vsdmClientService;
  @Autowired private PactConfig pactConfig;

  /**
   * Prevents VsdmZetaSdkClientConfig.vsdmServiceClient() from being called, which would fail due to
   * the missing smcb_private.p12 file in the test environment.
   */
  @MockitoBean private ZetaSdkClient zetaSdkClient;

  /**
   * Mocked so we can forward httpGet() calls to the Pact mock server using Java's built-in
   * HttpClient (no cleartext restrictions on JVM).
   */
  @MockitoBean private ZetaSdkClientAdapter zetaSdkClientAdapter;

  private SimulatorAttachedCard mockEgkCard;

  @BeforeEach
  void beforeEach() {
    System.setProperty("pact.consumer.name", pactConfig.getPactConsumerName());
    System.setProperty("pact.provider.name", pactConfig.getPactProviderName());
    MDC.put("traceId", "test-trace-12345");

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
    configureAdapterToCallMockServer(mockServer.getUrl());

    var result =
        vsdmClientService.requestVsd("terminalId", 1, mockEgkCard, "token123", "etag123", false);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isNotEmpty();
  }

  // Verify that on response 200 a valid Bundle FHIR resource is returned
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isBundle(MockServer mockServer) {
    configureAdapterToCallMockServer(mockServer.getUrl());

    var result =
        vsdmClientService.requestVsd("terminalId", 2, mockEgkCard, "token456", "etag456", false);

    JsonNode rootNode = new ObjectMapper().readTree(result.getBody());
    assertThat(rootNode.get("resourceType").asText()).isEqualTo("Bundle");
  }

  // Verify that on response 200 the returned Bundle is a VSDMBundle
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isVSDMBundle(MockServer mockServer) {
    configureAdapterToCallMockServer(mockServer.getUrl());

    var result =
        vsdmClientService.requestVsd("terminalId", 3, mockEgkCard, "token456", "etag456", false);

    JsonNode rootNode = new ObjectMapper().readTree(result.getBody());
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
    configureAdapterToCallMockServer(mockServer.getUrl());

    var result =
        vsdmClientService.requestVsd("terminalId", 4, mockEgkCard, "token789", "etag789", false);

    JsonNode entries = new ObjectMapper().readTree(result.getBody()).get("entry");
    assertThat(getResourceFromBundle(entries, "Patient").get("meta").get("profile").get(0).asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPatient");
  }

  // Verify that on response 200 the returned VSDMBundle includes a valid VSDMComposition resource
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isValidVSDMComposition(MockServer mockServer) {
    configureAdapterToCallMockServer(mockServer.getUrl());

    var result =
        vsdmClientService.requestVsd("terminalId", 5, mockEgkCard, "token101", "etag101", false);

    JsonNode entries = new ObjectMapper().readTree(result.getBody()).get("entry");
    assertThat(
            getResourceFromBundle(entries, "Composition")
                .get("meta")
                .get("profile")
                .get(0)
                .asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMComposition");
  }

  // Verify that on response 200 the returned VSDMBundle includes a valid Coverage resource
  @SneakyThrows
  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatient200isValidVSDMCoverage(MockServer mockServer) {
    configureAdapterToCallMockServer(mockServer.getUrl());

    var result =
        vsdmClientService.requestVsd("terminalId", 6, mockEgkCard, "token202", "etag202", false);

    JsonNode entries = new ObjectMapper().readTree(result.getBody()).get("entry");
    assertThat(
            getResourceFromBundle(entries, "Coverage").get("meta").get("profile").get(0).asText())
        .isEqualTo("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMCoverage");
  }

  /**
   * Configures the ZetaSdkClientAdapter mock to forward httpGet() calls to the Pact mock server
   * using Java's built-in HttpClient. This avoids any cleartext or TLS certificate restrictions
   * imposed by the Zeta SDK's Ktor client.
   *
   * @param baseUrl the URL of the Pact mock server (e.g. "http://localhost:12345")
   */
  @SneakyThrows
  private void configureAdapterToCallMockServer(String baseUrl) {
    doAnswer(
            invocation -> {
              String urlPath = invocation.getArgument(0);
              ZetaSdkClientAdapter.RequestParameters params = invocation.getArgument(1);

              HttpClient httpClient = HttpClient.newHttpClient();
              HttpRequest.Builder builder =
                  HttpRequest.newBuilder()
                      .uri(URI.create(baseUrl + "/" + urlPath))
                      .GET()
                      .header("PoPP", params.poppToken())
                      .header("x-trace-id", params.traceId())
                      .header(
                          "Accept",
                          params.isFhirXml() ? "application/fhir+xml" : "application/fhir+json");

              if (params.ifNoneMatch() != null && !params.ifNoneMatch().isBlank()) {
                builder.header("If-None-Match", params.ifNoneMatch());
              }

              HttpResponse<String> response =
                  httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

              Map<String, String> responseHeaders =
                  response.headers().map().entrySet().stream()
                      .filter(e -> !e.getValue().isEmpty())
                      .collect(
                          Collectors.toMap(
                              Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> a));

              return new ZetaSdkClientAdapter.Response(
                  HttpStatus.valueOf(response.statusCode()), responseHeaders, response.body());
            })
        .when(zetaSdkClientAdapter)
        .httpGet(any(), any());
  }

  /**
   * Get a resource from a Bundle by its resource type.
   *
   * @param entries the entries array of the Bundle
   * @param resourceType the FHIR resource type to look for
   * @return the matching resource node, or null if not found
   */
  private static @Nullable JsonNode getResourceFromBundle(JsonNode entries, String resourceType) {
    for (JsonNode entry : entries) {
      JsonNode resource = entry.get("resource");
      if (resource != null && resourceType.equals(resource.get("resourceType").asText())) {
        return resource;
      }
    }
    return null;
  }
}
