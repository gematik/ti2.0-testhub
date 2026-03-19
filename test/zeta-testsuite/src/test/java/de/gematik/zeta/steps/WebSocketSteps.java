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
/*-
 * #%L
 * ZETA Testsuite
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.zeta.services.PlainWebSocketSessionManager;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.springframework.web.socket.WebSocketHttpHeaders;

/** Cucumber steps for WebSocket (text frames) tests (non-STOMP), e.g. PoPP. */
@Slf4j
public class WebSocketSteps {

  private final PlainWebSocketSessionManager ws = new PlainWebSocketSessionManager();

  // Handshake headers for the next WS connection (e.g. Authorization for ZETA-PEP)
  private final Map<String, String> handshakeHeaders = new LinkedHashMap<>();

  @Gegebensei("setze Anfrage Timeout für WebSocket Verbindungen auf {int} Sekunden")
  @Gegebensei("setze Anfrage Timeout für plain WebSocket Verbindungen auf {int} Sekunden")
  @Given("set connection timeout for websocket connections to {int} seconds")
  @Given("set connection timeout for plain websocket connections to {int} seconds")
  public void setConnectionTimeout(int seconds) {
    PlainWebSocketSessionManager.setConnectionTimeoutSeconds(seconds);
  }

  @Gegebensei("setze Timeout für WebSocket Nachrichten auf {int} Sekunden")
  @Gegebensei("setze Timeout für plain WebSocket Nachrichten auf {int} Sekunden")
  @Given("set message timeout for websocket messages to {int} seconds")
  @Given("set message timeout for plain websocket messages to {int} seconds")
  public void setMessageTimeout(int seconds) {
    PlainWebSocketSessionManager.setMessageTimeoutSeconds(seconds);
  }

  @Gegebensei("setze HTTP Proxy für WebSocket auf {string}")
  @Given("set HTTP proxy for websocket to {string}")
  public void setWebSocketProxy(String proxyUri) {
    var resolvedProxy = TigerGlobalConfiguration.resolvePlaceholders(proxyUri);
    PlainWebSocketSessionManager.setProxyUri(resolvedProxy);
    log.info("WebSocket HTTP proxy set to: {}", resolvedProxy);
  }

  @Gegebensei("deaktiviere HTTP Proxy für WebSocket")
  @Given("disable HTTP proxy for websocket")
  public void disableWebSocketProxy() {
    PlainWebSocketSessionManager.setProxyUri(null);
    log.info("WebSocket HTTP proxy disabled");
  }

  @Wenn("eine WebSocket Verbindung zu {string} geöffnet wird")
  @Wenn("eine plain WebSocket Verbindung zu {string} geöffnet wird")
  @When("a websocket connection to {string} is opened")
  @When("a plain websocket connection to {string} is opened")
  public void open(String url) {
    var resolvedUrl = TigerGlobalConfiguration.resolvePlaceholders(url);
    ws.connect(resolvedUrl);
  }

  @Wenn("eine WebSocket Nachricht gesendet wird:")
  @Wenn("eine plain WebSocket Nachricht gesendet wird:")
  @When("a websocket message is sent:")
  @When("a plain websocket message is sent:")
  public void send(String payload) {
    ws.sendText(payload);
  }

  @Dann("wird eine WebSocket Nachricht empfangen")
  @Dann("wird eine plain WebSocket Nachricht empfangen")
  @Then("a websocket message is received")
  @Then("a plain websocket message is received")
  public void awaitMessage() {
    var msg = ws.awaitMessage();
    TigerGlobalConfiguration.putValue("LAST_WS_MESSAGE", msg);
    log.info("Stored LAST_WS_MESSAGE: {}", msg);
  }

  @Dann("enthält die letzte WebSocket Nachricht den Text {string}")
  @Dann("enthält die letzte plain WebSocket Nachricht den Text {string}")
  @Then("the last websocket message contains {string}")
  @Then("the last plain websocket message contains {string}")
  public void lastMessageContains(String expectedSubstring) {
    var msg = TigerGlobalConfiguration.readStringOptional("LAST_WS_MESSAGE").orElse(null);
    assertThat(msg).as("LAST_WS_MESSAGE is present").isNotNull();

    var resolvedExpected = TigerGlobalConfiguration.resolvePlaceholders(expectedSubstring);
    assertThat(msg).contains(resolvedExpected);
  }

  @Dann("wird die WebSocket Verbindung geschlossen")
  @Dann("wird die plain WebSocket Verbindung geschlossen")
  @Then("websocket connection is closed")
  @Then("plain websocket connection is closed")
  public void close() {
    ws.close();
  }

  @Gegebensei("setze WebSocket Handshake Header {string} auf {string}")
  @Gegebensei("setze plain WebSocket Handshake Header {string} auf {string}")
  @Given("set websocket handshake header {string} to {string}")
  @Given("set plain websocket handshake header {string} to {string}")
  public void setHandshakeHeader(String name, String value) {
    var resolvedName = TigerGlobalConfiguration.resolvePlaceholders(name);
    var resolvedValue = TigerGlobalConfiguration.resolvePlaceholders(value);
    handshakeHeaders.put(resolvedName, resolvedValue);
  }

  @Gegebensei("lösche alle WebSocket Handshake Header")
  @Gegebensei("lösche alle plain WebSocket Handshake Header")
  @Given("clear websocket handshake headers")
  @Given("clear plain websocket handshake headers")
  public void clearHandshakeHeaders() {
    handshakeHeaders.clear();
  }

  @Wenn("eine WebSocket Verbindung zu {string} mit den gesetzten Handshake Headern geöffnet wird")
  @Wenn(
      "eine plain WebSocket Verbindung zu {string} mit den gesetzten Handshake Headern geöffnet wird")
  @When("a websocket connection to {string} is opened with the configured handshake headers")
  @When("a plain websocket connection to {string} is opened with the configured handshake headers")
  public void openWithHandshakeHeaders(String url) {
    var resolvedUrl = TigerGlobalConfiguration.resolvePlaceholders(url);
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    handshakeHeaders.forEach(headers::add);
    ws.connect(resolvedUrl, headers);
  }

  @Wenn("eine WebSocket Verbindung zu {string} mit den gesetzten Handshake Headern fehlschlägt")
  @Wenn(
      "eine plain WebSocket Verbindung zu {string} mit den gesetzten Handshake Headern fehlschlägt")
  @When("a websocket connection to {string} fails with the configured handshake headers")
  @When("a plain websocket connection to {string} fails with the configured handshake headers")
  public void openWithHandshakeHeadersExpectFailure(String url) {
    Assertions.assertThatThrownBy(() -> openWithHandshakeHeaders(url))
        .as("WebSocket handshake should fail")
        .isInstanceOf(AssertionError.class);
  }
}
