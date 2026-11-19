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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class DockerHelperTest {

  @Test
  void waitsForHealthAndRefreshesReassignedPort() {
    List<String> ports = new ArrayList<>();
    AtomicInteger inspections = new AtomicInteger();
    DockerHelper.restartAndWaitUntilHealthy(
        "testhub-123-popp-client",
        8081,
        Duration.ofSeconds(2),
        command -> {
          if (command.get(1).equals("restart")) {
            assertThat(command).containsExactly("docker", "restart", "testhub-123-popp-client");
            return "";
          }
          assertThat(command)
              .containsExactly(
                  "docker", "inspect", "--format", "{{json .}}", "testhub-123-popp-client");
          String health = inspections.incrementAndGet() == 1 ? "starting" : "healthy";
          return """
          {"State":{"Running":true,"Health":{"Status":"%s"}},
           "NetworkSettings":{"Ports":{"8081/tcp":[
             {"HostIp":"0.0.0.0","HostPort":"33005"},
             {"HostIp":"::","HostPort":"33005"}]}}}
          """
              .formatted(health);
        },
        ports::add);
    assertThat(inspections.get()).isEqualTo(2);
    assertThat(ports).containsExactly("33005");
  }

  @Test
  void reportsStatePortsAndLogsWhenContainerExits() {
    Duration timeout = Duration.ofSeconds(2);
    Function<List<String>, String> commandRunner =
        command -> {
          if (command.get(1).equals("restart")) {
            return "";
          }
          if (command.get(1).equals("logs")) {
            assertThat(command).containsExactly("docker", "logs", "--tail", "100", "popp-client");
            return "Application startup failed";
          }
          if (command.get(3).equals("{{json .}}")) {
            return "{\"State\":{\"Running\":false,\"ExitCode\":1}}";
          }
          return "State=exited Ports=8081/tcp:33005";
        };
    Consumer<String> portUpdater =
        port -> {
          throw new AssertionError("Must not update port for failed container");
        };

    assertThatThrownBy(
            () ->
                DockerHelper.restartAndWaitUntilHealthy(
                    "popp-client", 8081, timeout, commandRunner, portUpdater))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("State=exited")
        .hasMessageContaining("33005")
        .hasMessageContaining("Application startup failed")
        .hasCauseInstanceOf(AssertionError.class);
  }

  @Test
  void reportsDiagnosticsWhenHealthWaitTimesOut() {
    Duration timeout = Duration.ofMillis(750);
    Function<List<String>, String> commandRunner =
        command -> {
          if (command.get(1).equals("restart")) {
            return "";
          }
          if (command.get(1).equals("logs")) {
            return "Still starting";
          }
          if (command.get(3).equals("{{json .}}")) {
            return "{\"State\":{\"Running\":true,\"Health\":{\"Status\":\"starting\"}}}";
          }
          return "State=starting Ports=80/tcp:33006";
        };
    Consumer<String> portUpdater =
        port -> {
          throw new AssertionError("Must not update port before healthy");
        };

    assertThatThrownBy(
            () ->
                DockerHelper.restartAndWaitUntilHealthy(
                    "vsdm-client", 80, timeout, commandRunner, portUpdater))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Still starting")
        .hasMessageContaining("33006")
        .hasCauseInstanceOf(org.awaitility.core.ConditionTimeoutException.class);
  }

  @Test
  void rejectsMissingOrAmbiguousPublishedPorts() throws Exception {
    for (String bindings :
        new String[] {"null", "[]", "[{\"HostPort\":\"33001\"},{\"HostPort\":\"33002\"}]"}) {
      var inspection =
          new ObjectMapper()
              .readTree("{\"NetworkSettings\":{\"Ports\":{\"80/tcp\":" + bindings + "}}}");
      assertThatThrownBy(() -> DockerHelper.publishedPort(inspection, 80))
          .isInstanceOf(AssertionError.class)
          .hasMessageContaining("Expected one published port");
    }
  }

  @Test
  void scopesCiLookupToProjectAndServiceRegardlessOfLocalName() {
    for (String service : new String[] {"vsdm-client", "popp-client", "otel-collector"}) {
      String name =
          DockerHelper.findRunningContainer(
              service,
              service,
              "testhub-123",
              command -> {
                assertThat(command)
                    .containsExactly(
                        "docker",
                        "ps",
                        "--filter",
                        "label=com.docker.compose.project=testhub-123",
                        "--filter",
                        "label=com.docker.compose.service=" + service,
                        "--format",
                        "{{.Names}}");
                return "  testhub-123-" + service + "  \n\n";
              });
      assertThat(name).isEqualTo("testhub-123-" + service);
    }
  }

  @Test
  void usesExactLocalNameWithoutProject() {
    for (String project : new String[] {null, "", "  "}) {
      assertThat(
              DockerHelper.findRunningContainer(
                  "vsdm-client",
                  "vsdm-client",
                  project,
                  command -> {
                    assertThat(command)
                        .containsExactly(
                            "docker",
                            "ps",
                            "--filter",
                            "name=^/\\Qvsdm-client\\E$",
                            "--format",
                            "{{.Names}}");
                    return "vsdm-client\n";
                  }))
          .isEqualTo("vsdm-client");
    }
  }

  @Test
  void usesServiceLabelLocallyForComposeGeneratedNames() {
    assertThat(
            DockerHelper.findRunningContainer(
                "otel-collector",
                null,
                null,
                command -> {
                  assertThat(command)
                      .containsExactly(
                          "docker",
                          "ps",
                          "--filter",
                          "label=com.docker.compose.service=otel-collector",
                          "--format",
                          "{{.Names}}");
                  return "testhub-backend-otel-collector-1\n";
                }))
        .isEqualTo("testhub-backend-otel-collector-1");
  }

  @Test
  void rejectsMissingContainer() {
    assertThatThrownBy(
            () ->
                DockerHelper.findRunningContainer(
                    "vsdm-client", "vsdm-client", "testhub-123", command -> "\n "))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("No running container")
        .hasMessageContaining("vsdm-client")
        .hasMessageContaining("testhub-123");
  }

  @Test
  void rejectsAmbiguousContainersInsteadOfPickingFirst() {
    assertThatThrownBy(
            () ->
                DockerHelper.findRunningContainer(
                    "otel-collector",
                    null,
                    null,
                    command -> "stack-a-otel-collector-1\nstack-b-otel-collector-1\n"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("Expected exactly one")
        .hasMessageContaining("stack-a-otel-collector-1")
        .hasMessageContaining("stack-b-otel-collector-1");
  }

  @Test
  void propagatesDockerCommandFailureWithoutNameFallback() {
    assertThatThrownBy(
            () ->
                DockerHelper.findRunningContainer(
                    "popp-client",
                    "popp-client",
                    null,
                    command -> {
                      throw new AssertionError("Docker daemon unavailable");
                    }))
        .isInstanceOf(AssertionError.class)
        .hasMessage("Docker daemon unavailable");
  }

  @Test
  void rejectsBlankServiceBeforeExecutingDocker() {
    for (String service : new String[] {null, "", " "}) {
      assertThatThrownBy(
              () ->
                  DockerHelper.findRunningContainer(
                      service,
                      null,
                      null,
                      command -> {
                        throw new AssertionError("Docker should not be invoked");
                      }))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must not be blank");
    }
  }
}
