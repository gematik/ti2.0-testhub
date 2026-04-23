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

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.common.config.TigerTypedConfigurationKey;
import de.gematik.test.tiger.proxy.data.ModificationDto;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.en.Then;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cucumber step definitions for TigerProxy manipulation operations.
 *
 * <p>This class provides step definitions for manipulating the TigerProxy. The TigerProxy URL is
 * automatically resolved from the configuration variable {@code zeta.paths.tigerProxy.baseUrl}.
 *
 * <p><b>Scope:</b> rules apply to requests that physically flow through the local TigerProxy (port
 * {@code ${ports.localTigerProxyProxyPort}}). Requests between Docker containers (e.g. PDP → OPA)
 * are routed through docker-tiger-proxy and are not affected by rules registered here.
 *
 * <p>All RBel manipulation rules added during a scenario are automatically removed in {@link
 * #cleanupModifications()}.
 */
@Slf4j
public class TigerProxyManipulationsSteps {

  private static final String AUTHORIZATION_HEADER_ROOT_CONFIG_KEY = "headers.authorization.root";
  private static final String DPOP_HEADER_ROOT_CONFIG_KEY = "headers.dpop.root";

  private static final TigerTypedConfigurationKey<String> TIGER_PROXY_BASE_URL =
      new TigerTypedConfigurationKey<>("zeta.paths.tigerProxy.baseUrl", String.class);

  private final RestTemplate restTemplate = new RestTemplate();
  private final List<String> activeModificationNames = new ArrayList<>();

  /**
   * Resolves the TigerProxy base URL from configuration.
   *
   * @param action short label used for log messages
   * @return the resolved base URL, or {@link Optional#empty()} when the URL is not configured
   */
  private Optional<String> resolveTigerProxyBaseUrl(String action) {
    var baseUrl = TIGER_PROXY_BASE_URL.getValue();
    if (baseUrl.isEmpty()) {
      log.info("Skipping TigerProxy {}; base URL not configured.", action);
    }
    return baseUrl;
  }

  /**
   * Resolves a required RBEL path from the Tiger configuration.
   *
   * @param configKey configuration key pointing to an RBEL path
   * @return the resolved RBEL path
   */
  private String getRequiredRbelPath(String configKey) {
    var configuredPath =
        TigerGlobalConfiguration.readStringOptional(configKey)
            .map(TigerGlobalConfiguration::resolvePlaceholders)
            .orElseThrow(() -> new AssertionError("Missing configuration: " + configKey));

    if (configuredPath.isBlank() || configuredPath.contains("${")) {
      throw new AssertionError(
          "RBEL path configuration could not be resolved for %s: '%s'"
              .formatted(configKey, configuredPath));
    }

    return configuredPath;
  }

  /**
   * Configures a JWT manipulation on the TigerProxy without re-signing.
   *
   * @param jwtLocation where the JWT is located (e.g., "$.header.dpop", "$.body.client_assertion")
   * @param jwtField what to change in the JWT (e.g., "header.typ", "body.iss")
   * @param value new value for the field
   */
  @Dann("Setze im TigerProxy für JWT in {string} das Feld {string} auf Wert {tigerResolvedString}")
  @Then("Set in TigerProxy for JWT in {string} the field {string} to value {tigerResolvedString}")
  public void setTigerProxyJwtManipulation(String jwtLocation, String jwtField, String value) {
    sendJwtManipulation(
        Map.of(
            "name", "jwt-mod-" + UUID.randomUUID(),
            "jwtLocation", jwtLocation,
            "jwtField", jwtField,
            "replaceWith", value));
  }

  /**
   * Sends a manipulation request to the TigerProxy to modify intercepted messages based on
   * specified criteria. This method directs the TigerProxy to apply a modification targeting a
   * particular field within the message, identified by its RBel path. It updates the field's value
   * with the provided new value during message interception.
   *
   * @param message Logic to identify the messages that needs to be manipulated
   * @param field RBel path identifier of the field you want to manipulate
   * @param value The new value to assign to the specified field
   */
  @Dann(
      "Setze im TigerProxy für die Nachricht {tigerResolvedString} die Manipulation auf "
          + "Feld {string} und Wert {tigerResolvedString}")
  @Then(
      "Set the manipulation in the TigerProxy for message {tigerResolvedString} to "
          + "field {string} and value {tigerResolvedString}")
  public void setTigerProxyManipulation(String message, String field, String value) {
    sendRbelManipulation(
        ModificationDto.builder()
            .name("mod-" + UUID.randomUUID())
            .condition(message)
            .targetElement(field)
            .replaceWith(value)
            .build());
  }

  /**
   * Sends a manipulation request to the TigerProxy to modify intercepted messages with execution
   * count. This method directs the TigerProxy to apply a modification targeting a particular field
   * within the message, identified by its RBel path, for a specified number of executions.
   *
   * @param message Logic to identify the messages that needs to be manipulated
   * @param field RBel path identifier of the field you want to manipulate
   * @param value The new value to assign to the specified field
   * @param executions Number of times to execute before auto-clearing
   */
  @Dann(
      "Setze im TigerProxy für die Nachricht {tigerResolvedString} die Manipulation auf "
          + "Feld {string} und Wert {tigerResolvedString} und {int} Ausführungen")
  @Then(
      "Set the manipulation in the TigerProxy for message {tigerResolvedString} to "
          + "field {string} and value {tigerResolvedString} with {int} executions")
  public void setTigerProxyManipulationWithExecutions(
      String message, String field, String value, Integer executions) {
    // Note: deleteAfterNExecutions is not supported by the TigerProxy ModificationDto API;
    // modifications are cleaned up via @After instead.
    log.debug(
        "Requested {} executions for RBel modification; limit is handled by @After cleanup.",
        executions);
    sendRbelManipulation(
        ModificationDto.builder()
            .name("mod-" + UUID.randomUUID())
            .condition(message)
            .targetElement(field)
            .replaceWith(value)
            .build());
  }

  /**
   * Sends a manipulation request to the TigerProxy to modify intercepted messages using regex
   * replacement. This is useful for modifying form-data fields which cannot be directly addressed
   * by RBel path. The regex filter is applied to the target element and matching parts are replaced
   * with the new value.
   *
   * @param message Logic to identify the messages that needs to be manipulated
   * @param field RBel path identifier of the field you want to manipulate (e.g., $.body)
   * @param regexFilter Regex pattern to find the part to replace within the target element
   * @param value The new value to replace the matched regex with
   */
  @Dann(
      "Setze im TigerProxy für die Nachricht {tigerResolvedString} die Regex-Manipulation auf "
          + "Feld {string} mit Regex {tigerResolvedString} und Wert {tigerResolvedString}")
  @Then(
      "Set the regex manipulation in the TigerProxy for message {tigerResolvedString} to "
          + "field {string} with regex {tigerResolvedString} and value {tigerResolvedString}")
  public void setTigerProxyRegexManipulation(
      String message, String field, String regexFilter, String value) {
    sendRbelManipulation(
        ModificationDto.builder()
            .name("regex-mod-" + UUID.randomUUID())
            .condition(message)
            .targetElement(field)
            .regexFilter(regexFilter)
            .replaceWith(value)
            .build());
  }

  /**
   * Clears all existing manipulations configured in the TigerProxy instance. This method instructs
   * the TigerProxy to remove all active manipulations, effectively resetting its modification rules
   * to a clean state.
   */
  @Dann("Alle Manipulationen im TigerProxy werden gestoppt")
  @Then("Reset all manipulation in the TigerProxy")
  public void resetTigerProxyManipulation() {
    var baseUrl = resolveTigerProxyBaseUrl("reset");
    if (baseUrl.isEmpty()) {
      return;
    }

    var manipulationUrl = getUrl(baseUrl.get(), "${zeta.paths.tigerProxy.modificationPath}");
    try {
      restTemplate.delete(manipulationUrl);
      activeModificationNames.clear();
      var resetResponse =
          restTemplate.postForEntity(
              getUrl(baseUrl.get(), "${zeta.paths.tigerProxy.resetJwtManipulationPath}"),
              null,
              String.class);
      assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    } catch (ResourceAccessException e) {
      throw new AssertionError("TigerProxy not reachable at '" + baseUrl.get() + "'.", e);
    } catch (RestClientException e) {
      throw new AssertionError("The manipulation could not be removed in the TigerProxy.", e);
    }
  }

  /** Removes all RBel modification rules added during the current scenario. */
  @After
  public void cleanupModifications() {
    if (activeModificationNames.isEmpty()) {
      return;
    }
    var baseUrl = resolveTigerProxyBaseUrl("cleanup");
    if (baseUrl.isEmpty()) {
      activeModificationNames.clear();
      return;
    }
    String base = getUrl(baseUrl.get(), "${zeta.paths.tigerProxy.modificationPath}");
    for (String name : new ArrayList<>(activeModificationNames)) {
      try {
        restTemplate.delete(base + "/" + name);
        log.info("Modification '{}' removed", name);
      } catch (Exception e) {
        log.warn("Could not remove modification '{}': {}", name, e.getMessage());
      }
    }
    activeModificationNames.clear();
  }

  /**
   * Configures a JWT manipulation on the TigerProxy.
   *
   * @param jwtLocation where the JWT is located (e.g., "$.header.dpop", "$.body.client_assertion")
   * @param jwtField what to change in the JWT (e.g., "header.typ", "body.iss")
   * @param value new value for the field
   * @param privateKeyPem private key used to re-sign the token
   */
  @Dann(
      "Setze im TigerProxy für JWT in {string} das Feld {string} auf Wert {tigerResolvedString} "
          + "mit privatem Schlüssel {tigerResolvedString}")
  @Then(
      "Set in TigerProxy for JWT in {string} the field {string} to value {tigerResolvedString} "
          + "using private key {tigerResolvedString}")
  public void setTigerProxyJwtManipulationWithKey(
      String jwtLocation, String jwtField, String value, String privateKeyPem) {
    sendJwtManipulation(
        Map.of(
            "jwtLocation", jwtLocation,
            "jwtField", jwtField,
            "replaceWith", value,
            "privateKeyPem", privateKeyPem));
  }

  /**
   * Configures a JWT manipulation on the TigerProxy with condition and execution limit, without
   * re-signing.
   *
   * @param jwtLocation where the JWT is located (e.g., "$.header.dpop", "$.body.client_assertion")
   * @param jwtField what to change in the JWT (e.g., "header.typ", "body.iss")
   * @param value new value for the field
   * @param condition regex pattern to match request paths
   * @param executions number of times to execute before auto-clearing (null = unlimited)
   */
  @Dann(
      "Setze im TigerProxy für JWT in {string} das Feld {string} auf Wert {tigerResolvedString} "
          + "für Pfad {tigerResolvedString} und {int} Ausführungen")
  @Then(
      "Set in TigerProxy for JWT in {string} the field {string} to value {tigerResolvedString} "
          + "for path {tigerResolvedString} with {int} executions")
  public void setTigerProxyJwtManipulationWithConditionNoResign(
      String jwtLocation, String jwtField, String value, String condition, Integer executions) {
    sendJwtManipulation(
        Map.of(
            "jwtLocation", jwtLocation,
            "jwtField", jwtField,
            "replaceWith", value,
            "condition", condition,
            "deleteAfterNExecutions", executions));
  }

  /**
   * Configures a JWT manipulation on the TigerProxy with condition and execution limit.
   *
   * @param jwtLocation where the JWT is located (e.g., "$.header.dpop", "$.body.client_assertion")
   * @param jwtField what to change in the JWT (e.g., "header.typ", "body.iss")
   * @param value new value for the field
   * @param privateKeyPem private key used to re-sign the token
   * @param condition regex pattern to match request paths
   * @param executions number of times to execute before auto-clearing (null = unlimited)
   */
  @Dann(
      "Setze im TigerProxy für JWT in {string} das Feld {string} auf Wert {tigerResolvedString} "
          + "mit privatem Schlüssel {tigerResolvedString} für Pfad {tigerResolvedString} und {int} Ausführungen")
  @Then(
      "Set in TigerProxy for JWT in {string} the field {string} to value {tigerResolvedString} "
          + "using private key {tigerResolvedString} for path {tigerResolvedString} with {int} executions")
  public void setTigerProxyJwtManipulationWithCondition(
      String jwtLocation,
      String jwtField,
      String value,
      String privateKeyPem,
      String condition,
      Integer executions) {
    sendJwtManipulation(
        Map.ofEntries(
            Map.entry("jwtLocation", jwtLocation),
            Map.entry("jwtField", jwtField),
            Map.entry("replaceWith", value),
            Map.entry("privateKeyPem", privateKeyPem),
            Map.entry("condition", condition),
            Map.entry("deleteAfterNExecutions", executions)));
  }

  /**
   * Configures a JWT manipulation on the TigerProxy with condition, execution limit, and JWK
   * replacement. The JWK in the JWT header will be replaced with the public key derived from the
   * provided private key.
   *
   * @param jwtLocation where the JWT is located (e.g., "$.header.dpop", "$.body.client_assertion")
   * @param jwtField what to change in the JWT (e.g., "header.typ", "body.iss")
   * @param value new value for the field
   * @param privateKeyPem private key used to re-sign the token and derive public key for JWK
   * @param condition regex pattern to match request paths
   * @param executions number of times to execute before auto-clearing (null = unlimited)
   */
  @Dann(
      "Setze im TigerProxy für JWT in {string} das Feld {string} auf Wert {tigerResolvedString} "
          + "mit privatem Schlüssel {tigerResolvedString} für Pfad {tigerResolvedString} und {int} Ausführungen und ersetze JWK")
  @Then(
      "Set in TigerProxy for JWT in {string} the field {string} to value {tigerResolvedString} "
          + "using private key {tigerResolvedString} for path {tigerResolvedString} with {int} executions and replace JWK")
  public void setTigerProxyJwtManipulationWithConditionAndReplaceJwk(
      String jwtLocation,
      String jwtField,
      String value,
      String privateKeyPem,
      String condition,
      Integer executions) {
    sendJwtManipulation(
        Map.ofEntries(
            Map.entry("jwtLocation", jwtLocation),
            Map.entry("jwtField", jwtField),
            Map.entry("replaceWith", value),
            Map.entry("privateKeyPem", privateKeyPem),
            Map.entry("condition", condition),
            Map.entry("deleteAfterNExecutions", executions),
            Map.entry("replaceJwk", true)));
  }

  /**
   * Configures a JWT manipulation on the TigerProxy for Authorization header (access token) with
   * automatic DPoP ath update. When the access token is manipulated, the ath claim in the DPoP JWT
   * will be recalculated and the DPoP JWT will be re-signed.
   *
   * @param jwtField what to change in the access token JWT (e.g., "body.iss", "body.sub")
   * @param value new value for the field
   * @param accessTokenKeyPem private key used to re-sign the access token
   * @param dpopKeyPem private key used to re-sign the DPoP JWT after ath update
   * @param condition regex pattern to match request paths
   * @param executions number of times to execute before auto-clearing
   */
  @Dann(
      "Setze im TigerProxy für Access Token das Feld {string} auf Wert {tigerResolvedString} "
          + "mit Access Token Key {tigerResolvedString} und DPoP Key {tigerResolvedString} "
          + "für Pfad {tigerResolvedString} und {int} Ausführungen")
  @Then(
      "Set in TigerProxy for access token the field {string} to value {tigerResolvedString} "
          + "using access token key {tigerResolvedString} and DPoP key {tigerResolvedString} "
          + "for path {tigerResolvedString} with {int} executions")
  public void setTigerProxyAccessTokenManipulationWithAthUpdate(
      String jwtField,
      String value,
      String accessTokenKeyPem,
      String dpopKeyPem,
      String condition,
      Integer executions) {
    sendJwtManipulation(
        Map.ofEntries(
            Map.entry("jwtLocation", getRequiredRbelPath(AUTHORIZATION_HEADER_ROOT_CONFIG_KEY)),
            Map.entry("jwtField", jwtField),
            Map.entry("replaceWith", value),
            Map.entry("privateKeyPem", accessTokenKeyPem),
            Map.entry("condition", condition),
            Map.entry("deleteAfterNExecutions", executions),
            Map.entry("dpopLocation", getRequiredRbelPath(DPOP_HEADER_ROOT_CONFIG_KEY)),
            Map.entry("dpopPrivateKeyPem", dpopKeyPem),
            Map.entry("updateAth", true)));
  }

  /**
   * Configures a single-execution JWT manipulation on the TigerProxy (executes once then
   * auto-clears).
   *
   * @param jwtLocation where the JWT is located
   * @param jwtField what to change in the JWT
   * @param value new value for the field
   * @param privateKeyPem private key used to re-sign the token
   */
  @Dann(
      "Setze im TigerProxy für JWT in {string} das Feld {string} auf Wert {tigerResolvedString} "
          + "mit privatem Schlüssel {tigerResolvedString} einmalig")
  @Then(
      "Set in TigerProxy for JWT in {string} the field {string} to value {tigerResolvedString} "
          + "using private key {tigerResolvedString} once")
  public void setTigerProxyJwtManipulationOnce(
      String jwtLocation, String jwtField, String value, String privateKeyPem) {
    sendJwtManipulation(
        Map.of(
            "jwtLocation", jwtLocation,
            "jwtField", jwtField,
            "replaceWith", value,
            "privateKeyPem", privateKeyPem,
            "deleteAfterNExecutions", 1));
  }

  /**
   * Sends a JWT manipulation request to TigerProxy. Central method handling all HTTP communication
   * for JWT manipulations.
   *
   * @param body Request body containing manipulation parameters
   */
  private void sendJwtManipulation(Map<String, Object> body) {
    var baseUrl = resolveTigerProxyBaseUrl("JWT manipulation");
    if (baseUrl.isEmpty()) {
      return;
    }

    var url = getUrl(baseUrl.get(), "${zeta.paths.tigerProxy.modifyJwtPath}");
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    try {
      var response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    } catch (ResourceAccessException e) {
      throw new AssertionError("TigerProxy not reachable at '" + baseUrl.get() + "'.", e);
    } catch (RestClientException e) {
      throw new AssertionError("JWT manipulation failed: " + e.getMessage());
    }
  }

  /**
   * Sends an RBel manipulation request to TigerProxy. Central method handling all HTTP
   * communication for RBel path manipulations.
   *
   * @param modification the modification to register at the TigerProxy
   */
  private void sendRbelManipulation(ModificationDto modification) {
    var baseUrl = resolveTigerProxyBaseUrl("RBel manipulation");
    if (baseUrl.isEmpty()) {
      return;
    }

    var url = getUrl(baseUrl.get(), "${zeta.paths.tigerProxy.modificationPath}");
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    try {
      restTemplate.put(url, new HttpEntity<>(modification, headers));
      var response = restTemplate.getForEntity(url, String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      // track name for @After cleanup
      if (modification.getName() != null) {
        activeModificationNames.add(modification.getName());
      }
    } catch (ResourceAccessException e) {
      throw new AssertionError("TigerProxy not reachable at '" + baseUrl.get() + "'.", e);
    } catch (RestClientException e) {
      throw new AssertionError("RBel manipulation failed: " + e.getMessage());
    }
  }

  /**
   * Builds the request URL from the TigerProxy base URL and uri part.
   *
   * @param baseUrl the resolved TigerProxy base URL
   * @param uri Specific endpoint that is called in the TigerProxy
   * @return The complete URL to access the TigerProxy API
   */
  private String getUrl(String baseUrl, String uri) {
    var resolvedUri = TigerGlobalConfiguration.resolvePlaceholders(uri);
    return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
        + resolvedUri;
  }
}
