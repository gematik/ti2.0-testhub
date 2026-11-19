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
package de.gematik.zeta.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

/** Docker operations scoped to the current Compose project, without caching container names. */
@Slf4j
public final class DockerHelper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private DockerHelper() {}

  /** Finds a service by Compose labels, requiring exactly one running container. */
  public static String findRunningContainer(String serviceName) {
    return findRunningContainer(serviceName, null);
  }

  /**
   * Uses project/service labels in CI and an exact local container name when no project is set.
   * Pass {@code null} for the local name to use the service label for Compose-generated names.
   */
  public static String findRunningContainer(String serviceName, String localContainerName) {
    return findRunningContainer(
        serviceName,
        localContainerName,
        System.getenv("COMPOSE_PROJECT_NAME"),
        DockerHelper::runCommand);
  }

  /**
   * Testable variant of {@link #findRunningContainer(String, String)} with injectable environment.
   *
   * @param serviceName Docker Compose service name (value of the {@code com.docker.compose.service}
   *     label)
   * @param localContainerName exact container name used locally when no project is set, or {@code
   *     null}
   * @param projectName Compose project name; when non-blank the lookup is scoped to it
   * @param commandRunner executes the {@code docker} command and returns its output
   * @return the name of the single running container
   * @throws IllegalArgumentException if {@code serviceName} is blank
   * @throws AssertionError if no container, or more than one, matches
   */
  static String findRunningContainer(
      String serviceName,
      String localContainerName,
      String projectName,
      Function<List<String>, String> commandRunner) {
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("Docker Compose service name must not be blank");
    }
    List<String> command = new ArrayList<>(List.of("docker", "ps", "--filter"));
    String scope;
    if (projectName != null && !projectName.isBlank()) {
      command.add("label=com.docker.compose.project=" + projectName);
      command.add("--filter");
      command.add("label=com.docker.compose.service=" + serviceName);
      scope = "Compose project '%s'".formatted(projectName);
    } else if (localContainerName != null && !localContainerName.isBlank()) {
      command.add("name=^/" + Pattern.quote(localContainerName) + "$");
      scope = "local container name '%s'".formatted(localContainerName);
    } else {
      command.add("label=com.docker.compose.service=" + serviceName);
      scope = "Compose service label (COMPOSE_PROJECT_NAME is unset)";
    }
    command.addAll(List.of("--format", "{{.Names}}"));
    List<String> names =
        commandRunner
            .apply(List.copyOf(command))
            .lines()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    if (names.isEmpty()) {
      throw new AssertionError(
          "No running container for Docker Compose service '%s' found for %s. Is the stack up?"
              .formatted(serviceName, scope));
    }
    if (names.size() > 1) {
      throw new AssertionError(
          "Expected exactly one running container for Docker Compose service '%s' for %s, found %s: %s"
              .formatted(serviceName, scope, names.size(), names));
    }
    return names.get(0);
  }

  /**
   * Restarts the running container of a Compose service without waiting for it to become healthy.
   *
   * @param serviceName Docker Compose service name
   * @param localContainerName exact container name used locally when no project is set, or {@code
   *     null}
   */
  public static void restartService(String serviceName, String localContainerName) {
    String containerName = findRunningContainer(serviceName, localContainerName);
    runCommand(List.of("docker", "restart", containerName));
  }

  /**
   * Waits for the container's configured health check, then refreshes its published port. Docker
   * can assign a different host port after restarting a container created with host port zero.
   *
   * @param serviceName Docker Compose service name
   * @param localContainerName exact container name used locally when no project is set, or {@code
   *     null}
   * @param internalPort container port whose published host port is read
   * @param portConfigurationKey Tiger configuration key (e.g. {@code ports.poppClientPort}) that
   *     receives the refreshed host port
   * @param timeout maximum time to wait for the container to report {@code healthy}
   * @throws AssertionError if the container stops, becomes unhealthy, has no health check, or the
   *     timeout elapses; the message includes container state and recent logs
   */
  public static void restartServiceAndWaitUntilHealthy(
      String serviceName,
      String localContainerName,
      int internalPort,
      String portConfigurationKey,
      Duration timeout) {
    String containerName = findRunningContainer(serviceName, localContainerName);
    restartAndWaitUntilHealthy(
        containerName,
        internalPort,
        timeout,
        DockerHelper::runCommand,
        port -> TigerGlobalConfiguration.putValue(portConfigurationKey, port));
  }

  /**
   * Testable variant of {@link #restartServiceAndWaitUntilHealthy} with injectable collaborators.
   *
   * @param commandRunner executes {@code docker} commands and returns their output
   * @param portUpdater receives the published host port once the container is healthy
   */
  static void restartAndWaitUntilHealthy(
      String containerName,
      int internalPort,
      Duration timeout,
      Function<List<String>, String> commandRunner,
      Consumer<String> portUpdater) {
    commandRunner.apply(List.of("docker", "restart", containerName));
    try {
      Awaitility.await()
          .pollDelay(Duration.ZERO)
          .pollInterval(Duration.ofMillis(500))
          .atMost(timeout)
          .until(
              () -> {
                JsonNode inspection = inspectContainer(containerName, commandRunner);
                JsonNode state = inspection.path("State");
                String health = state.path("Health").path("Status").asText();
                if (!state.path("Running").asBoolean()
                    || "unhealthy".equals(health)
                    || health.isEmpty()) {
                  throw new AssertionError(
                      "Container '%s' cannot become ready: %s".formatted(containerName, state));
                }
                if (!"healthy".equals(health)) {
                  return false;
                }
                portUpdater.accept(publishedPort(inspection, internalPort));
                return true;
              });
      log.info("Container '{}' is healthy; published port refreshed", containerName);
    } catch (ConditionTimeoutException | AssertionError e) {
      throw new AssertionError(
          "Container '%s' did not become healthy within %s after restart.%n%s"
              .formatted(
                  containerName, timeout, containerDiagnostics(containerName, commandRunner)),
          e);
    }
  }

  /** Runs {@code docker inspect} and parses the result as a JSON object. */
  private static JsonNode inspectContainer(
      String containerName, Function<List<String>, String> commandRunner) {
    String output =
        commandRunner.apply(List.of("docker", "inspect", "--format", "{{json .}}", containerName));
    try {
      JsonNode inspection = OBJECT_MAPPER.readTree(output);
      if (inspection == null || !inspection.isObject()) {
        throw new AssertionError("Missing Docker inspect object for " + containerName);
      }
      return inspection;
    } catch (JsonProcessingException e) {
      throw new AssertionError("Invalid Docker inspect response for " + containerName, e);
    }
  }

  /**
   * Extracts the host port bound to {@code internalPort/tcp} from a {@code docker inspect} result.
   *
   * @return the host port as a string
   * @throws AssertionError if there is not exactly one distinct binding or the port is not within
   *     1-65535
   */
  static String publishedPort(JsonNode inspection, int internalPort) {
    JsonNode bindings =
        inspection.path("NetworkSettings").path("Ports").path(internalPort + "/tcp");
    Set<String> ports = new LinkedHashSet<>();
    for (JsonNode binding : bindings) {
      String port = binding.path("HostPort").asText();
      if (!port.matches("[0-9]{1,5}")
          || Integer.parseInt(port) < 1
          || Integer.parseInt(port) > 65535) {
        throw new AssertionError("Invalid published port: " + binding);
      }
      ports.add(port);
    }
    if (ports.size() != 1) {
      throw new AssertionError(
          "Expected one published port for %s/tcp, found: %s".formatted(internalPort, bindings));
    }
    return ports.iterator().next();
  }

  /** Collects container state, port mappings and the last 100 log lines for failure messages. */
  private static String containerDiagnostics(
      String containerName, Function<List<String>, String> commandRunner) {
    String state =
        commandRunner.apply(
            List.of(
                "docker",
                "inspect",
                "--format",
                "State={{json .State}} Ports={{json .NetworkSettings.Ports}}",
                containerName));
    String logs = commandRunner.apply(List.of("docker", "logs", "--tail", "100", containerName));
    return state + "\nRecent container logs:\n" + logs;
  }

  /**
   * Returns the container's logs (stdout and stderr) written since the given instant.
   *
   * @param containerName actual container name, e.g. from {@link #findRunningContainer(String)}
   * @param since lower bound passed to {@code docker logs --since}
   */
  public static String logsSince(String containerName, Instant since) {
    return runCommand(List.of("docker", "logs", "--since", since.toString(), containerName));
  }

  /**
   * Runs a command with a 60 second timeout and returns its combined stdout/stderr.
   *
   * @throws AssertionError on timeout, non-zero exit code or I/O failure
   */
  static String runCommand(List<String> command) {
    Process process = null;
    try {
      // File redirection avoids pipe-buffer deadlocks and keeps the timeout effective for large
      // logs.
      Path outputFile = Files.createTempFile("zeta-docker-", ".log");
      try {
        process =
            new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(outputFile.toFile())
                .start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
          throw new AssertionError(
              "Docker command '%s' timed out after 60 seconds."
                  .formatted(String.join(" ", command)));
        }
        String output = Files.readString(outputFile, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
          throw new AssertionError(
              "Docker command '%s' failed (exit %s). Output: %s"
                  .formatted(String.join(" ", command), process.exitValue(), output));
        }
        log.debug("Docker command '{}' succeeded", String.join(" ", command));
        return output;
      } finally {
        if (process != null && process.isAlive()) {
          process.destroyForcibly();
        }
        Files.deleteIfExists(outputFile);
      }
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new AssertionError("Failed to run docker command: " + String.join(" ", command), e);
    }
  }
}
