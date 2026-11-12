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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/** Cucumber steps for provisioning the card terminal simulator with card images (eGK, SMC-B). */
@Slf4j
public class CardTerminalSteps {

  private final RestTemplate restTemplate = new RestTemplate();

  @Gegebensei("das Kartenterminal {tigerResolvedString} ist am VSDM-Client konfiguriert")
  @Given("card terminal {tigerResolvedString} is configured at the VSDM client")
  public void configureTerminalAtVsdmClient(String terminalUrl) {
    String vsdmClientUrl =
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://${ports.host}:${ports.vsdmClientPort}");

    String terminalConfig =
        "[{\"name\":\"Terminal-0\",\"type\":\"SIMSVC\",\"url\":\"%s\"}]".formatted(terminalUrl);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(terminalConfig, headers);

    String url = vsdmClientUrl + "/client/config/terminal";
    log.info("Configuring terminal at VSDM client: {} -> {}", url, terminalConfig);
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("Terminal configuration should succeed (status=%s)", response.getStatusCode())
        .isTrue();
    log.info("Terminal configured (status={})", response.getStatusCode());

    // Give the VSDM client time to establish the WebSocket connection to the card terminal
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Gegebensei("die Karte {string} ist in Slot {int} des Kartenterminals geladen")
  @Given("card {string} is loaded in slot {int} of the card terminal")
  public void loadCardInSlot(String cardImagePath, int slotId) {
    String cardTerminalUrl =
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://${ports.host}:${ports.cardTerminalPort}");

    // Remove any existing card from the slot first (ignore 404 if empty)
    String slotUrl = cardTerminalUrl + "/slots/" + slotId;
    try {
      restTemplate.delete(slotUrl);
      log.info("Removed existing card from slot {}", slotId);
    } catch (Exception e) {
      log.debug("No card to remove from slot {} ({})", slotId, e.getMessage());
    }

    // Resolve path relative to project root (tiger.rootFolder)
    String rootFolder = TigerGlobalConfiguration.resolvePlaceholders("${tiger.rootFolder|.}");
    Path resolvedPath = Path.of(rootFolder).resolve(cardImagePath).normalize();
    String cardImageContent = loadFile(resolvedPath);

    HttpHeaders headers = new HttpHeaders();
    if (cardImagePath.endsWith(".json")) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    } else {
      headers.setContentType(MediaType.APPLICATION_XML);
    }

    HttpEntity<String> request = new HttpEntity<>(cardImageContent, headers);
    String url = cardTerminalUrl + "/slots/" + slotId;

    log.info("Loading card image '{}' into slot {} at {}", resolvedPath, slotId, url);
    ResponseEntity<String> response =
        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

    assertThat(response.getStatusCode().is2xxSuccessful())
        .as(
            "Card image '%s' should be loaded into slot %d (status=%s, body=%s)",
            resolvedPath, slotId, response.getStatusCode(), response.getBody())
        .isTrue();
    log.info("Card loaded in slot {} (status={})", slotId, response.getStatusCode());
  }

  private String loadFile(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      throw new AssertionError("Failed to read file: " + path, e);
    }
  }
}
