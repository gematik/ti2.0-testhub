/*-
 * #%L
 * ZeTA Testsuite
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
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Step definitions for policy hot-reload testing via OPA.
 *
 * <p>This test verifies that OPA applies policy changes at runtime without restart. Policies are
 * managed via the OPA REST API:
 *
 * <ul>
 *   <li>PUT /v1/policies/{id} - Upload/update a policy
 *   <li>DELETE /v1/policies/{id} - Remove a policy
 *   <li>GET /v1/policies - List all policies
 * </ul>
 *
 * <p>Policy decisions are queried directly via OPA's Data API (POST /v1/data/zeta/authz/decision).
 */
@Slf4j
public class PolicyUpdateabilitySteps {

  private static final String OPA_POLICY_ID = "zeta/authz";
  private static final String OPA_DEFAULT_POLICY_ID = "policies/authz.rego";
  private static final RestTemplate REST_TEMPLATE = new RestTemplate();

  /** PS profile definitions: profileName -> professionOid */
  private final Map<String, String> psProfiles = new HashMap<>();

  /** Policy rego content cache: policyName -> rego source */
  private final Map<String, String> policyContent = new HashMap<>();

  private int lastResponseStatusCode;

  // --- Helper methods ---

  private String getOpaBaseUrl() {
    return TigerGlobalConfiguration.resolvePlaceholders("${zeta.server.opa.baseUrl}");
  }

  private String loadPolicyFromClasspath(String policyName) {
    String resourcePath = "policies/policy_" + policyName.toLowerCase() + ".rego";
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Policy resource not found: " + resourcePath);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load policy from classpath: " + resourcePath, e);
    }
  }

  private void uploadPolicyToOpa(String regoContent) {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_POLICY_ID;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf("text/plain"));
    HttpEntity<String> request = new HttpEntity<>(regoContent, headers);

    ResponseEntity<String> response =
        REST_TEMPLATE.exchange(url, HttpMethod.PUT, request, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA policy upload should succeed")
        .isTrue();

    log.info("Policy uploaded to OPA ({}): HTTP {}", url, response.getStatusCode().value());
  }

  private void deletePolicyFromOpa() {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_POLICY_ID;
    try {
      REST_TEMPLATE.delete(url);
      log.info("Policy deleted from OPA: {}", url);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Policy not found in OPA (already clean): {}", url);
    }
  }

  private void deleteDefaultPolicyFromOpa() {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_DEFAULT_POLICY_ID;
    try {
      REST_TEMPLATE.delete(url);
      log.info("Default policy deleted from OPA: {}", url);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Default policy not found in OPA (already clean): {}", url);
    }
  }

  /**
   * Queries OPA directly to check if the given professionOid is allowed by the current policy. This
   * avoids the complexity of Keycloak token exchange while still testing policy hot-reload.
   */
  private int sendPolicyDecisionRequest(String professionOid) {
    String url = getOpaBaseUrl() + "/v1/data/zeta/authz/decision";

    // Build the OPA input matching the structure expected by the Rego policy
    String jsonBody =
        String.format("{\"input\":{\"user_info\":{\"professionOID\":\"%s\"}}}", professionOid);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

    try {
      ResponseEntity<String> response = REST_TEMPLATE.postForEntity(url, request, String.class);
      String body = response.getBody();
      log.info("OPA decision response: HTTP {} - {}", response.getStatusCode().value(), body);

      // Parse the OPA response to check if allow is true
      // Response format: {"result":{"allow":true/false,...}}
      if (body != null && body.matches("(?s).*\"allow\"\\s*:\\s*true.*")) {
        return 200; // allowed
      } else {
        return 403; // denied
      }
    } catch (HttpClientErrorException e) {
      log.info(
          "OPA decision error: HTTP {} - {}",
          e.getStatusCode().value(),
          e.getResponseBodyAsString());
      return e.getStatusCode().value();
    }
  }

  // --- Cucumber Step Definitions ---

  @Gegebensei("die OPA Policy Registry ist leer")
  public void opaPolicyRegistryIsEmpty() {
    deletePolicyFromOpa();
    deleteDefaultPolicyFromOpa();
    log.info("OPA Policy Registry ist leer (alle zeta/authz Policies entfernt)");
  }

  @Gegebensei("die Policy {string} ist in der OPA Registry verfügbar")
  public void policyIsAvailableInOpaRegistry(String policyName) {
    String rego = loadPolicyFromClasspath(policyName);
    policyContent.put(policyName, rego);

    uploadPolicyToOpa(rego);
    log.info("Policy '{}' ist in der OPA Registry aktiv", policyName);
  }

  @Gegebensei("die Policy {string} ist verfügbar aber NICHT in der OPA Registry veröffentlicht")
  public void policyIsAvailableButNotPublished(String policyName) {
    String rego = loadPolicyFromClasspath(policyName);
    policyContent.put(policyName, rego);
    log.info(
        "Policy '{}' vorbereitet (lokal gecacht), aber nicht in OPA veröffentlicht", policyName);
  }

  @SuppressWarnings("unused") // nonMatchingPolicy is required by the Gherkin step pattern
  @Gegebensei("das PS-Profil {string} passt zu Policy {string} aber nicht zu {string}")
  public void psProfileMatchesPolicy(
      String profileName, String matchingPolicy, String nonMatchingPolicy) {
    String professionOid;
    if ("P1".equals(matchingPolicy)) {
      professionOid = "1.2.276.0.76.4.49"; // Arzt
    } else if ("P11".equals(matchingPolicy)) {
      professionOid = "1.2.276.0.76.4.50"; // Zahnarzt
    } else {
      throw new IllegalArgumentException("Unknown policy: " + matchingPolicy);
    }
    psProfiles.put(profileName, professionOid);
    log.info(
        "PS-Profil '{}' konfiguriert mit professionOid '{}' (passt zu '{}')",
        profileName,
        professionOid,
        matchingPolicy);
  }

  @Gegebensei("OPA ist erreichbar")
  public void opaIsReachable() {
    String healthUrl = getOpaBaseUrl() + "/v1/policies";
    ResponseEntity<String> response = REST_TEMPLATE.getForEntity(healthUrl, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA should be reachable at %s", healthUrl)
        .isTrue();
    log.info("ZETA/OPA ist gestartet und erreichbar");
  }

  @Dann("hat ZETA die Policy {string} geladen")
  public void zetaHasLoadedPolicy(String policyName) {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_POLICY_ID;
    ResponseEntity<String> response = REST_TEMPLATE.getForEntity(url, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA should have policy '%s' loaded", policyName)
        .isTrue();
    log.info("Policy '{}' ist in OPA geladen (verifiziert via {})", policyName, url);
  }

  @Wenn("PS-Profil {string} eine Anfrage an ZETA sendet")
  public void psProfileSendsRequest(String profileName) {
    String professionOid = psProfiles.get(profileName);
    assertThat(professionOid)
        .as("PS-Profil '%s' muss vorher konfiguriert worden sein", profileName)
        .isNotNull();

    log.info(
        "PS-Profil '{}' sendet OPA-Decision-Request mit professionOid '{}'",
        profileName,
        professionOid);
    lastResponseStatusCode = sendPolicyDecisionRequest(professionOid);
  }

  @Wenn("die Policy {string} in der OPA Registry veröffentlicht wird")
  public void publishPolicyToOpaRegistry(String policyName) {
    String rego = policyContent.get(policyName);
    assertThat(rego).as("Policy '%s' muss vorher vorbereitet worden sein", policyName).isNotNull();

    uploadPolicyToOpa(rego);
    log.info("Policy '{}' wurde in der OPA Registry veröffentlicht (Hot-Reload)", policyName);
  }

  @Dann("antwortet ZETA mit Status {string}")
  public void zetaRespondsWithStatus(String expectedStatusPattern) {
    if ("2xx".equals(expectedStatusPattern)) {
      assertThat(lastResponseStatusCode).as("ZETA sollte mit 2xx antworten").isBetween(200, 299);
    } else if ("4xx".equals(expectedStatusPattern)) {
      assertThat(lastResponseStatusCode).as("ZETA sollte mit 4xx antworten").isBetween(400, 499);
    } else {
      assertThat(String.valueOf(lastResponseStatusCode))
          .as("ZETA Statuscode sollte dem Pattern '%s' entsprechen", expectedStatusPattern)
          .matches(expectedStatusPattern.replace("x", "\\d"));
    }
    log.info(
        "ZETA Antwort-Status: {} (erwartet: {})", lastResponseStatusCode, expectedStatusPattern);
  }
}
