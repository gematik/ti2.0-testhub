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
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.en.Given;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cucumber steps that reset ZeTA-PDP-side client-registration state so that a subsequent {@code
 * vsdRequest} performs a genuinely fresh Dynamic Client Registration (DCR) and therefore a fresh
 * OPA policy decision.
 *
 * <p><b>Background:</b> The ZeTA-PDP (Keycloak) only calls OPA once per SMC-B identity, during the
 * initial DCR. The resulting Keycloak client and the VSDM-Client's in-memory access/refresh token
 * are then reused for all further {@code vsdRequest} calls, bypassing OPA entirely. To reliably
 * exercise an OPA policy decision (e.g. to verify rejections via TigerProxy manipulation, see
 * {@link TigerProxyManipulationsSteps}) in every test run - regardless of what earlier scenarios or
 * previous runs may have already registered - both the persisted Keycloak client and the VSDM
 * client's in-memory token cache must be cleared beforehand.
 *
 * <p><b>Local-only:</b> This relies on Docker-only capabilities (Keycloak admin REST API, `docker
 * restart`) that are not available against real/RU-DEV infrastructure. Scenarios using these steps
 * must be tagged {@code @local}.
 */
@Slf4j
public class PolicyRejectionSteps {

  private static final Set<String> KEYCLOAK_BUILTIN_CLIENT_IDS =
      Set.of(
          "account",
          "account-console",
          "admin-cli",
          "broker",
          "realm-management",
          "security-admin-console");

  private static final String ZETA_GUARD_REALM = "zeta-guard";
  private static final String VSDM_CLIENT_CONTAINER_NAME = "vsdm-client";

  private final RestTemplate restTemplate = new RestTemplate();

  @Gegebensei("die ZeTA-PDP-Registrierung des VSDM-Clients ist vollständig zurückgesetzt")
  @Given("the ZeTA-PDP registration of the VSDM client is fully reset")
  public void resetZetaPdpRegistration() {
    deleteDynamicallyRegisteredClients();
    restartVsdmClient();
  }

  /**
   * Deletes all non-builtin (i.e. dynamically self-registered via DCR) Keycloak clients from the
   * {@code zeta-guard} realm, forcing the next {@code vsdRequest} to perform a fresh registration
   * (and therefore a fresh OPA decision).
   */
  private void deleteDynamicallyRegisteredClients() {
    String pdpBaseUrl =
        TigerGlobalConfiguration.resolvePlaceholders("${zeta.paths.vsdm.pdp.baseUrl}");
    String adminToken = fetchAdminAccessToken(pdpBaseUrl);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    String clientsUrl = pdpBaseUrl + "/auth/admin/realms/" + ZETA_GUARD_REALM + "/clients";

    ResponseEntity<List<Map<String, Object>>> response =
        restTemplate.exchange(
            clientsUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});
    List<Map<String, Object>> clients = response.getBody();
    assertThat(clients).as("Keycloak clients list should be available").isNotNull();

    Set<String> deletedClientIds = new LinkedHashSet<>();
    for (Map<String, Object> client : clients) {
      Object clientId = client.get("clientId");
      Object id = client.get("id");
      if (clientId instanceof String clientIdStr
          && id instanceof String idStr
          && !KEYCLOAK_BUILTIN_CLIENT_IDS.contains(clientIdStr)) {
        restTemplate.exchange(
            clientsUrl + "/" + idStr, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        deletedClientIds.add(clientIdStr);
      }
    }
    log.info(
        "Deleted {} dynamically-registered ZeTA-PDP client(s): {}",
        deletedClientIds.size(),
        deletedClientIds);
  }

  private String fetchAdminAccessToken(String pdpBaseUrl) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", "admin-cli");
    form.add("username", "admin");
    form.add("password", "admin");
    form.add("grant_type", "password");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(
            pdpBaseUrl + "/auth/realms/master/protocol/openid-connect/token",
            HttpMethod.POST,
            new HttpEntity<>(form, headers),
            new ParameterizedTypeReference<>() {});

    Object accessToken = response.getBody() != null ? response.getBody().get("access_token") : null;
    assertThat(accessToken)
        .as("Keycloak admin access token should have been issued")
        .isInstanceOf(String.class);
    return (String) accessToken;
  }

  /**
   * Restarts the {@code vsdm-client} Docker container via the Docker CLI to clear its in-memory
   * access/refresh token cache, then waits for the service to become healthy again.
   */
  private void restartVsdmClient() {
    List<String> command = List.of("docker", "restart", VSDM_CLIENT_CONTAINER_NAME);
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      boolean finished = process.waitFor(60, TimeUnit.SECONDS);
      String output = new String(process.getInputStream().readAllBytes());
      if (!finished || process.exitValue() != 0) {
        throw new AssertionError(
            "Docker command '%s' failed or timed out. Output: %s"
                .formatted(String.join(" ", command), output));
      }
      log.info("Docker command '{}' succeeded: {}", String.join(" ", command), output.trim());
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new AssertionError("Failed to run docker command: " + String.join(" ", command), e);
    }
    waitForVsdmClientHealthy();
  }

  @SuppressWarnings("BusyWait")
  private void waitForVsdmClientHealthy() {
    String statusUrl =
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://127.0.0.1:${ports.vsdmClientPort}/service/status");
    Duration timeout = Duration.ofSeconds(60);
    Instant deadline = Instant.now().plus(timeout);

    while (Instant.now().isBefore(deadline)) {
      try {
        ResponseEntity<String> response = restTemplate.getForEntity(statusUrl, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
          log.info("VSDM client is healthy again after restart.");
          return;
        }
      } catch (RestClientException e) {
        log.debug("VSDM client not ready yet: {}", e.getMessage());
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for VSDM client to become healthy", e);
      }
    }
    throw new AssertionError(
        "VSDM client did not become healthy within %s after restart.".formatted(timeout));
  }
}
