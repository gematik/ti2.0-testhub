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
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Cucumber steps that reset ZeTA-PDP-side client-registration state so that a subsequent
 * popp-client {@code /token} call performs a genuinely fresh Service Discovery and Dynamic Client
 * Registration (DCR).
 *
 * <p><b>Background:</b> The popp-client (ZeTA SDK) only performs Service Discovery + DCR once per
 * container lifetime, during its very first {@code /token} interaction. Its resulting client_id and
 * access/refresh tokens are then cached in-memory and reused for all further {@code /token} calls,
 * bypassing discovery/DCR entirely. To reliably exercise discovery/DCR in every test run -
 * regardless of what earlier scenarios in the same suite run may already have triggered - both the
 * persisted Keycloak client on the popp-PDP side and the popp-client's in-memory cache must be
 * cleared beforehand (analogous to {@link PolicyRejectionSteps} on the VSDM side).
 *
 * <p><b>Local-only:</b> {@link #resetZetaPdpRegistration()} relies on Docker-only capabilities
 * (Keycloak admin REST API, `docker restart`) that are not available against real/RU-DEV
 * infrastructure. Scenarios using that step must be tagged {@code @local}.
 *
 * <p>{@link #restartPoppClientToClearTokenCache()} only uses `docker restart` (no Keycloak admin
 * API call) and is therefore safe to use even when popp-client authenticates against a real, remote
 * PDP (e.g. Arvato/TK), as long as the popp-client container itself still runs locally via Docker
 * Compose - which it always does, since {@code ${popp.client.tokenUrl}} is a local container
 * endpoint.
 */
@Slf4j
public class PoppClientResetSteps {

  private static final Set<String> KEYCLOAK_BUILTIN_CLIENT_IDS =
      Set.of(
          "account",
          "account-console",
          "admin-cli",
          "broker",
          "realm-management",
          "security-admin-console");

  private static final String ZETA_GUARD_REALM = "zeta-guard";
  private static final String POPP_CLIENT_CONTAINER_NAME = "popp-client";

  private final RestTemplate restTemplate = new RestTemplate();

  @Gegebensei("die ZeTA-PDP-Registrierung des popp-client ist vollständig zurückgesetzt")
  public void resetZetaPdpRegistration() {
    deleteDynamicallyRegisteredClients();
    restartPoppClient();
  }

  /**
   * Restarts the popp-client container to clear its in-memory ZeTA client_id/token cache, without
   * touching any PDP-side client registration.
   *
   * <p>Unlike {@link #resetZetaPdpRegistration()}, this does NOT call the Keycloak admin REST API
   * (which only exists for the locally-mocked PDP, see {@code zeta.paths.popp.pdp.baseUrl}). It
   * relies only on {@code docker restart}, which the popp-client container already depends on
   * regardless of which PDP it authenticates against (local mock, Arvato, or TK), since {@code
   * ${popp.client.tokenUrl}} always points at the locally-run popp-client container. This makes it
   * safe to use against a real/remote PDP (e.g. in {@code smcb_authentisierung.feature}), where a
   * fresh DCR registration is simply accepted by the PDP without requiring prior cleanup.
   */
  @Gegebensei("der popp-client wurde neu gestartet, um seinen ZeTA-Token-Cache zu leeren")
  public void restartPoppClientToClearTokenCache() {
    restartPoppClient();
  }

  /**
   * Deletes all non-builtin (i.e. dynamically self-registered via DCR) Keycloak clients from the
   * {@code zeta-guard} realm on the popp-PDP, forcing the next popp-client {@code /token} call to
   * perform a fresh Service Discovery + DCR.
   */
  private void deleteDynamicallyRegisteredClients() {
    String pdpBaseUrl =
        TigerGlobalConfiguration.resolvePlaceholders("${zeta.paths.popp.pdp.baseUrl}");
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
        "Deleted {} dynamically-registered popp-PDP client(s): {}",
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
   * Restarts the {@code popp-client} Docker container via the Docker CLI to clear its in-memory
   * client_id/token cache, then waits for the service to become healthy again.
   */
  private void restartPoppClient() {
    List<String> command = List.of("docker", "restart", POPP_CLIENT_CONTAINER_NAME);
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
    waitForPoppClientHealthy();
  }

  private void waitForPoppClientHealthy() {
    String statusUrl =
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://${ports.host}:${ports.poppClientPort}/actuator/health");
    Duration timeout = Duration.ofSeconds(60);

    try {
      Awaitility.await()
          .atMost(timeout)
          .pollInterval(Duration.ofSeconds(1))
          .ignoreExceptions()
          .until(
              () ->
                  restTemplate
                      .getForEntity(statusUrl, String.class)
                      .getStatusCode()
                      .is2xxSuccessful());
      log.info("popp-client is healthy again after restart.");
    } catch (ConditionTimeoutException e) {
      throw new AssertionError(
          "popp-client did not become healthy within %s after restart.".formatted(timeout), e);
    }
  }
}
