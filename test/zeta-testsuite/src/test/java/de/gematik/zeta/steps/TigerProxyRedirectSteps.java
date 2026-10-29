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
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.web.client.RestTemplate;

/**
 * Cucumber step definitions that redirect a single hostname's DNS resolution inside one Docker
 * container to a different container's current IP address, by editing that container's {@code
 * /etc/hosts} at runtime. Used to make the ZeTA PEP observe a different source IP for a request
 * (see {@code docker-tiger-proxy-b} in {@code compose-local.yaml}), e.g. for the IP-binding /
 * "no-travel" tests (A_28827/A_28803). Local-only; scenarios must be tagged {@code @local}.
 *
 * <p>{@code docker-tiger-proxy-b} is started/stopped on demand via {@link
 * #redirectHostnameToContainer} / {@link #cleanupRedirects()} rather than always running.
 */
@Slf4j
public class TigerProxyRedirectSteps {

  private static final String DOCKER_TIGER_PROXY_B_SERVICE = "docker-tiger-proxy-b";
  private static final String DOCKER_TIGER_PROXY_B_ADMIN_HEALTH_URL =
      "http://${ports.host}:${ports.remoteTigerProxyBAdminPort}/route";

  private final List<String[]> activeOverrides = new ArrayList<>();
  private final RestTemplate restTemplate = new RestTemplate();
  private boolean dockerTigerProxyBStartedByThisScenario = false;

  /**
   * Pins {@code hostname} to the current Docker network IP of {@code targetContainer} via {@code
   * /etc/hosts} in {@code container}.
   */
  @Dann(
      "Der Hostname {string} wird im Container {string} auf die IP des Containers {string}"
          + " umgeleitet")
  @Then("The hostname {string} is redirected in container {string} to the IP of container {string}")
  public void redirectHostnameToContainer(
      String hostname, String container, String targetContainer) {
    if (DOCKER_TIGER_PROXY_B_SERVICE.equals(targetContainer)) {
      ensureDockerTigerProxyBRunning();
    }
    String targetIp = resolveContainerIp(targetContainer);
    assertThat(targetIp).as("Docker network IP of container '%s'", targetContainer).isNotBlank();

    runDockerCommand(
        List.of(
            "docker",
            "exec",
            container,
            "sh",
            "-c",
            "echo '" + targetIp + " " + hostname + "' >> /etc/hosts"));
    activeOverrides.add(new String[] {container, hostname});
    log.info(
        "Redirected hostname '{}' in container '{}' to '{}' (container '{}').",
        hostname,
        container,
        targetIp,
        targetContainer);
  }

  /** Removes a previously added {@code /etc/hosts} redirect for {@code hostname}. */
  @Wenn("die Hostnamen-Umleitung für {string} im Container {string} wird aufgehoben")
  @When("the hostname redirect for {string} in container {string} is removed")
  public void removeHostnameRedirect(String hostname, String container) {
    removeHostsEntry(container, hostname);
    activeOverrides.removeIf(entry -> entry[0].equals(container) && entry[1].equals(hostname));
    log.info("Removed hostname redirect for '{}' in container '{}'.", hostname, container);
  }

  private String resolveContainerIp(String container) {
    List<String> command =
        List.of(
            "docker",
            "inspect",
            "-f",
            "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
            container);
    return runDockerCommand(command).trim();
  }

  /**
   * Starts {@code docker-tiger-proxy-b} via {@code docker compose up -d} if not already running.
   */
  private void ensureDockerTigerProxyBRunning() {
    if (isContainerRunning()) {
      return;
    }
    runDockerCommand(
        List.of(
            "docker",
            "compose",
            "-f",
            resolveComposeFilePath(),
            "up",
            "-d",
            DOCKER_TIGER_PROXY_B_SERVICE));
    dockerTigerProxyBStartedByThisScenario = true;
    waitForDockerTigerProxyBHealthy();
    log.info("Started '{}' on demand for this scenario.", DOCKER_TIGER_PROXY_B_SERVICE);
  }

  /** Stops {@code docker-tiger-proxy-b} again if this scenario started it. */
  private void stopDockerTigerProxyBIfStartedByThisScenario() {
    if (!dockerTigerProxyBStartedByThisScenario) {
      return;
    }
    try {
      runDockerCommand(
          List.of(
              "docker",
              "compose",
              "-f",
              resolveComposeFilePath(),
              "stop",
              DOCKER_TIGER_PROXY_B_SERVICE));
      log.info("Stopped '{}' again after this scenario.", DOCKER_TIGER_PROXY_B_SERVICE);
    } finally {
      dockerTigerProxyBStartedByThisScenario = false;
    }
  }

  private boolean isContainerRunning() {
    List<String> command =
        List.of("docker", "inspect", "-f", "{{.State.Running}}", DOCKER_TIGER_PROXY_B_SERVICE);
    try {
      return Boolean.parseBoolean(runDockerCommand(command).trim());
    } catch (AssertionError e) {
      return false;
    }
  }

  private String resolveComposeFilePath() {
    String rootFolder = TigerGlobalConfiguration.resolvePlaceholders("${tiger.rootFolder|.}");
    return rootFolder + "/infra/docker/compose-local.yaml";
  }

  /** Waits until docker-tiger-proxy-b's admin REST API responds. */
  private void waitForDockerTigerProxyBHealthy() {
    String healthUrl =
        TigerGlobalConfiguration.resolvePlaceholders(DOCKER_TIGER_PROXY_B_ADMIN_HEALTH_URL);
    try {
      Awaitility.await()
          .atMost(Duration.ofSeconds(60))
          .pollInterval(Duration.ofSeconds(1))
          .ignoreExceptions()
          .until(
              () ->
                  restTemplate
                      .getForEntity(healthUrl, String.class)
                      .getStatusCode()
                      .is2xxSuccessful());
    } catch (ConditionTimeoutException e) {
      throw new AssertionError(
          "'%s' did not become healthy within 60s after starting it on demand."
              .formatted(DOCKER_TIGER_PROXY_B_SERVICE),
          e);
    }
  }

  private void removeHostsEntry(String container, String hostname) {
    runDockerCommand(
        List.of(
            "docker",
            "exec",
            container,
            "sh",
            "-c",
            "grep -v '[[:space:]]"
                + hostname
                + "$' /etc/hosts > /tmp/hosts.redirect-cleanup"
                + " && cat /tmp/hosts.redirect-cleanup > /etc/hosts"
                + " && rm -f /tmp/hosts.redirect-cleanup"));
  }

  private String runDockerCommand(List<String> command) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      boolean finished = process.waitFor(30, TimeUnit.SECONDS);
      String output = new String(process.getInputStream().readAllBytes());
      if (!finished || process.exitValue() != 0) {
        throw new AssertionError(
            "Docker command '%s' failed or timed out. Output: %s"
                .formatted(String.join(" ", command), output));
      }
      return output;
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new AssertionError("Failed to run docker command: " + String.join(" ", command), e);
    }
  }

  /** Safety net: removes any redirects that a scenario forgot to clean up explicitly. */
  @After
  public void cleanupRedirects() {
    for (String[] entry : new ArrayList<>(activeOverrides)) {
      try {
        removeHostsEntry(entry[0], entry[1]);
        log.info(
            "Cleaned up leftover hostname redirect for '{}' in container '{}'.",
            entry[1],
            entry[0]);
      } catch (Exception e) {
        log.warn(
            "Could not clean up hostname redirect for '{}' in container '{}': {}",
            entry[1],
            entry[0],
            e.getMessage());
      }
    }
    activeOverrides.clear();

    try {
      stopDockerTigerProxyBIfStartedByThisScenario();
    } catch (Exception e) {
      log.warn(
          "Could not stop '{}' after scenario: {}", DOCKER_TIGER_PROXY_B_SERVICE, e.getMessage());
    }
  }
}
