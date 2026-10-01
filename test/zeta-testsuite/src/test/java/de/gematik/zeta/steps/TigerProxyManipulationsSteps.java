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
import de.gematik.test.tiger.common.config.TigerTypedConfigurationKey;
import de.gematik.test.tiger.proxy.data.ModificationDto;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.en.Then;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
   * Clears all existing manipulations configured in the TigerProxy instance. This method instructs
   * the TigerProxy to remove all active manipulations, effectively resetting its modification rules
   * to a clean state.
   *
   * <p>Note: The TigerProxy's {@code /modification} endpoint only supports {@code GET} (list) and
   * {@code DELETE /modification/{name}} (delete by name) - there is no bulk-delete endpoint. A bare
   * {@code DELETE /modification} therefore fails with {@code 405 Method Not Allowed}. This method
   * instead lists all currently registered modifications and deletes each of them by name.
   *
   * <p>Note: {@code ${zeta.paths.tigerProxy.resetJwtManipulationPath}} ({@code
   * /api/rbel/jwtManipulation/reset}) does not exist on the TigerProxy (verified via decompilation
   * of tiger-proxy 4.4.0/4.4.2: no such controller/endpoint is present) and always returns {@code
   * 404}. Deleting each modification by name (above) already fully resets the TigerProxy's
   * modification rules, so no additional call is needed here.
   */
  @Dann("Alle Manipulationen im TigerProxy werden gestoppt")
  @Then("Reset all manipulation in the TigerProxy")
  public void resetTigerProxyManipulation() {
    var baseUrl = resolveTigerProxyBaseUrl("reset");
    if (baseUrl.isEmpty()) {
      return;
    }

    var manipulationUrl = getUrl(baseUrl.get());
    try {
      List<ModificationDto> modifications = fetchModifications(manipulationUrl);
      for (ModificationDto modification : modifications) {
        restTemplate.delete(manipulationUrl + "/" + modification.getName());
      }
      activeModificationNames.clear();
    } catch (ResourceAccessException e) {
      throw new AssertionError("TigerProxy not reachable at '" + baseUrl.get() + "'.", e);
    } catch (RestClientException e) {
      throw new AssertionError("The manipulation could not be removed in the TigerProxy.", e);
    }
  }

  private List<ModificationDto> fetchModifications(String manipulationUrl) {
    ResponseEntity<ModificationDto[]> response =
        restTemplate.getForEntity(manipulationUrl, ModificationDto[].class);
    ModificationDto[] body = response.getBody();
    return body == null ? List.of() : List.of(body);
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
    String base = getUrl(baseUrl.get());
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

    var url = getUrl(baseUrl.get());
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
   * Builds the request URL for the TigerProxy modification endpoint.
   *
   * @param baseUrl the resolved TigerProxy base URL
   * @return The complete URL to access the TigerProxy modification API
   */
  private String getUrl(String baseUrl) {
    var resolvedUri =
        TigerGlobalConfiguration.resolvePlaceholders("${zeta.paths.tigerProxy.modificationPath}");
    return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
        + resolvedUri;
  }
}
