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
import static org.awaitility.Awaitility.await;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.zeta.services.DockerHelper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.de.Und;
import io.cucumber.java.en.Then;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused") // Cucumber glue methods are invoked via reflection
@Slf4j
public class OtelLoggingSteps {

  private static final Duration LOG_SEARCH_TIMEOUT = Duration.ofSeconds(15);

  /**
   * Zeitpunkt, ab dem die Container-Logs durchsucht werden ("--since"). Wird vor jedem Szenario mit
   * einem kleinen Sicherheitspuffer gesetzt, damit unabhängig vom Log-Volumen des Containers (z. B.
   * durch Health-Check-Traces) immer der relevante Zeitraum abgedeckt wird, statt sich auf eine
   * fixe Zeilenanzahl ("--tail") zu verlassen, die bei hohem Log-Aufkommen zu schnell überschrieben
   * werden kann.
   */
  private Instant scenarioStartTime;

  @Before
  public void rememberScenarioStartTime() {
    scenarioStartTime = Instant.now().minusSeconds(2);
  }

  @Und(
      "die Fehlermeldung mit Statuscode {string} ist in den Logs des Containers {string} vorhanden")
  @Then("the error message with status code {string} is present in the logs of container {string}")
  public void verifyStatusCodeInContainerLogs(String statusCode, String containerName) {
    verifyLogPatternInContainer(containerName, statusCodePatterns(statusCode));
  }

  @Und(
      "das Fehler-Event {string} oder Statuscode {string} ist in den Logs des Containers {string}"
          + " vorhanden")
  @Then(
      "the error event {string} or status code {string} is present in the logs of container"
          + " {string}")
  public void verifyErrorEventInContainerLogs(
      String eventName, String statusCode, String containerName) {
    List<String> patterns = new ArrayList<>();
    patterns.add(eventName);
    patterns.addAll(statusCodePatterns(statusCode));
    verifyLogPatternInContainer(containerName, patterns);
  }

  /**
   * Prüft, dass ALLE in der DataTable angegebenen Telemetrie-Attribute (z. B. {@code service.name},
   * {@code http.method}, {@code http.route}) gemeinsam in den Logs des Otel-Collectors auftauchen.
   * Jede Zeile der Tabelle ist ein Teilstring, wie er im "detailed"-Debug-Exporter ausgegeben wird
   * (z. B. {@code http.method: Str(GET)}). Enthaltene {@code ${...}}-Platzhalter werden vorab über
   * die Tiger-Konfiguration aufgelöst.
   */
  @Und("die folgenden Telemetrie-Attribute sind in den Logs des Containers {string} vorhanden:")
  @Then("the following telemetry attributes are present in the logs of container {string}:")
  public void verifyTelemetryAttributesInContainerLogs(String containerName, DataTable dataTable) {
    List<String> expectedPatterns =
        dataTable.asList(String.class).stream()
            .map(TigerGlobalConfiguration::resolvePlaceholders)
            .toList();
    verifyAllLogPatternsInContainer(containerName, expectedPatterns);
  }

  /**
   * Erzeugt eindeutige Suchmuster für einen HTTP-Statuscode anhand der tatsächlichen
   * OTLP-Attributnamen (z. B. "http.response.status_code: Int(400)"), statt die nackte Zahl als
   * Substring zu suchen. Eine nackte Zahl wie "401" trifft sonst zufällig auch auf
   * Nanosekunden-Zeitstempel oder Trace-/Span-IDs (Hex-Strings) und erzeugt Fehlalarme.
   */
  private List<String> statusCodePatterns(String statusCode) {
    return List.of(
        "http.response.status_code: Int(" + statusCode + ")",
        "http.status_code: Int(" + statusCode + ")");
  }

  private void verifyLogPatternInContainer(
      String containerNameSubstring, List<String> expectedPatterns) {
    await()
        .atMost(LOG_SEARCH_TIMEOUT)
        .pollInterval(Duration.ofSeconds(1))
        .until(
            () -> {
              String resolvedContainerName =
                  DockerHelper.findRunningContainer(containerNameSubstring);
              String latestLogs = DockerHelper.logsSince(resolvedContainerName, scenarioStartTime);
              return expectedPatterns.stream()
                  .anyMatch(
                      pattern -> {
                        if (latestLogs.contains(pattern)) {
                          log.info(
                              "Gefundenes Telemetrie-Muster '{}' im Container '{}'",
                              pattern,
                              resolvedContainerName);
                          return true;
                        }
                        return false;
                      });
            });
  }

  /**
   * Wie {@link #verifyLogPatternInContainer}, aber mit UND-Verknüpfung: es müssen ALLE angegebenen
   * Muster gemeinsam in den Logs vorkommen, bevor der Check erfolgreich ist.
   */
  private void verifyAllLogPatternsInContainer(
      String containerNameSubstring, List<String> expectedPatterns) {
    await()
        .atMost(LOG_SEARCH_TIMEOUT)
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              String resolvedContainerName =
                  DockerHelper.findRunningContainer(containerNameSubstring);
              String latestLogs = DockerHelper.logsSince(resolvedContainerName, scenarioStartTime);
              List<String> missingPatterns =
                  expectedPatterns.stream()
                      .filter(pattern -> !latestLogs.contains(pattern))
                      .toList();

              assertThat(missingPatterns)
                  .as(
                      "Folgende erwartete Telemetrie-Attribute wurden in den Logs von %s nicht"
                          + " gefunden. Letzte Logs:\n"
                          + "%s",
                      resolvedContainerName, latestLogs)
                  .isEmpty();
              log.info(
                  "Alle erwarteten Telemetrie-Attribute {} im Container '{}' gefunden",
                  expectedPatterns,
                  resolvedContainerName);
            });
  }
}
