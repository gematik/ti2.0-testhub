/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.zeta.services.KeycloakClientAssertionFactory;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.When;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Cucumber step definitions for ZETA client registration and token exchange against the real
 * Keycloak PDP (not the mock).
 *
 * <p>These steps generate an RSA-2048 keypair at runtime, register the public key via DCR, and sign
 * the {@code client_assertion} JWT with the private key (RS256).
 */
@Slf4j
public class KeycloakDcrTokenExchangeSteps {

  private static final ObjectMapper JSON = new ObjectMapper();

  /**
   * Registers a new client at the Keycloak PDP via Dynamic Client Registration using the SMC-B
   * public key in the JWKS. Stores the {@code client_id} in the Tiger variable {@code
   * KEYCLOAK_CLIENT_ID}.
   */
  @Wenn("registriere Client {string} mit SMC-B-Schlüssel an {string} über Tiger-Proxy {string}")
  @When("register client {string} with SMC-B key at {string} via Tiger proxy {string}")
  public void registerClientWithSmcbKey(
      String clientName, String registerEndpoint, String proxyUrl) {
    String resolvedEndpoint = TigerGlobalConfiguration.resolvePlaceholders(registerEndpoint);
    String resolvedProxy = TigerGlobalConfiguration.resolvePlaceholders(proxyUrl);

    URI targetUri = URI.create(resolvedEndpoint);
    URI proxyUri = URI.create(resolvedProxy);

    // Build DCR request body with SMC-B JWKS
    String dcrBody = KeycloakClientAssertionFactory.createDcrRequestBody(clientName);
    log.info("DCR request body: {}", dcrBody);

    // Send via Tiger proxy
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setProxy(
        new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));

    RestTemplate rt = new RestTemplate(factory);
    // Don't throw on 4xx/5xx – let Tiger proxy record the response for TGR assertions
    rt.setErrorHandler(noOpErrorHandler());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(dcrBody, headers);

    ResponseEntity<String> response = rt.postForEntity(targetUri, request, String.class);

    log.info("DCR response status: {}", response.getStatusCode());

    // Extract client_id and store in Tiger context for subsequent steps
    try {
      JsonNode body = JSON.readTree(response.getBody());
      String clientId = body.get("client_id").asText();
      TigerGlobalConfiguration.putValue("KEYCLOAK_CLIENT_ID", clientId);
      log.info("Registered client_id: {}", clientId);

      if (body.has("registration_access_token")) {
        TigerGlobalConfiguration.putValue(
            "KEYCLOAK_REGISTRATION_ACCESS_TOKEN", body.get("registration_access_token").asText());
      }
    } catch (Exception e) {
      log.warn("Could not extract client_id from DCR response: {}", e.getMessage());
    }
  }

  /**
   * Performs a Token Exchange against the real Keycloak PDP. Requires a prior client registration
   * (client_id stored in {@code KEYCLOAK_CLIENT_ID}).
   *
   * <p>Flow:
   *
   * <ol>
   *   <li>Generates a PoPP token via the PoPP token generator
   *   <li>Creates a {@code client_assertion} JWT signed with the SMC-B key
   *   <li>Sends the token exchange request to Keycloak
   * </ol>
   */
  @Wenn(
      "sende Keycloak-Token-Exchange-Request an {string} mit Audience {string} und PoPP-Token von"
          + " {string} über Tiger-Proxy {string}")
  @When(
      "send Keycloak token exchange request to {string} with audience {string} and PoPP token from"
          + " {string} via Tiger proxy {string}")
  public void sendKeycloakTokenExchange(
      String tokenEndpoint, String audience, String poppServerUrl, String proxyUrl) {
    String resolvedTokenEndpoint = TigerGlobalConfiguration.resolvePlaceholders(tokenEndpoint);
    String resolvedAudience = TigerGlobalConfiguration.resolvePlaceholders(audience);
    String resolvedPoppServer = TigerGlobalConfiguration.resolvePlaceholders(poppServerUrl);
    String resolvedProxy = TigerGlobalConfiguration.resolvePlaceholders(proxyUrl);
    String clientId = TigerGlobalConfiguration.resolvePlaceholders("${KEYCLOAK_CLIENT_ID}");

    // 1. Generate PoPP token
    String poppToken = generatePoppToken(resolvedPoppServer);
    log.info(
        "PoPP token generated: {}...", poppToken.substring(0, Math.min(50, poppToken.length())));

    // 2. Create client_assertion JWT (aud = Keycloak issuer URL)
    String clientAssertion =
        KeycloakClientAssertionFactory.createClientAssertion(clientId, resolvedAudience);
    log.info("client_assertion created for client_id={}", clientId);

    // 3. Create DPoP proof JWT (RFC 9449)
    String dpopProof =
        KeycloakClientAssertionFactory.createDpopProof("POST", resolvedTokenEndpoint);
    log.info("DPoP proof created for POST {}", resolvedTokenEndpoint);

    // 4. Build token exchange form body
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");
    form.add("subject_token", poppToken);
    form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
    form.add("client_assertion", clientAssertion);
    form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
    form.add("client_id", clientId);

    // 5. Send via Tiger proxy
    URI targetUri = URI.create(resolvedTokenEndpoint);
    URI proxyUri = URI.create(resolvedProxy);

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setProxy(
        new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));

    RestTemplate rt = new RestTemplate(factory);
    // Don't throw on 4xx/5xx – let Tiger proxy record the response for TGR assertions
    rt.setErrorHandler(noOpErrorHandler());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("DPoP", dpopProof);
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    rt.postForEntity(targetUri, request, String.class);
    // Response is recorded by Tiger proxy and can be validated via TGR steps
  }

  /** Returns a no-op error handler that never throws – errors are validated via TGR steps. */
  private static ResponseErrorHandler noOpErrorHandler() {
    return response -> false;
  }

  /** Generates a PoPP token via the PoPP token generator REST API. */
  private String generatePoppToken(String poppServerBaseUrl) {
    URI uri = URI.create(poppServerBaseUrl + "/popp/test/api/v1/token-generator");

    long now = System.currentTimeMillis() / 1000;
    String requestBody =
        """
        {
          "tokenParamsList": [{
            "proofMethod": "ehc-practitioner-trustedchannel",
            "patientProofTime": "%d",
            "iat": "%d",
            "patientId": "X123456789",
            "insurerId": "123456789",
            "actorId": "1-2012345678",
            "actorProfessionOid": "1.2.276.0.76.4.50"
          }]
        }
        """
            .formatted(now, now);

    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    var response = rt.postForEntity(uri, request, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("PoPP token generator should return 2xx")
        .isTrue();

    try {
      JsonNode node = JSON.readTree(response.getBody());
      JsonNode tokenResults = node.get("tokenResults");
      assertThat(tokenResults).as("Response must contain tokenResults").isNotNull();
      assertThat(tokenResults.isArray() && !tokenResults.isEmpty())
          .as("tokenResults must be non-empty array")
          .isTrue();
      return tokenResults.get(0).asText();
    } catch (Exception e) {
      throw new AssertionError("Failed to parse PoPP token generator response: " + e.getMessage());
    }
  }
}
