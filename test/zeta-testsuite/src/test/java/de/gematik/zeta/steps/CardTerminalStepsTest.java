/*-
 * #%L
 * ZeTA Testsuite
 * %%
 * Copyright (C) 2026 gematik GmbH
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

import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class CardTerminalStepsTest {

  @TempDir Path directory;

  @Test
  void provisionsClientAndCardTerminalOnConfiguredDockerHost() throws IOException {
    var steps = new CardTerminalSteps();
    var restTemplate = (RestTemplate) ReflectionTestUtils.getField(steps, "restTemplate");
    var server = MockRestServiceServer.createServer(restTemplate);
    String clientUrl = "http://docker.internal:26100";
    String terminalUrl = "http://docker.internal:26200";
    server
        .expect(requestTo(clientUrl + "/client/config/terminal"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());
    server
        .expect(requestTo(terminalUrl + "/slots/1"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withSuccess());
    server
        .expect(requestTo(terminalUrl + "/slots/1"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());
    Files.writeString(directory.resolve("card.xml"), "<card/>");

    try (var configuration = mockStatic(TigerGlobalConfiguration.class)) {
      configuration
          .when(
              () ->
                  TigerGlobalConfiguration.resolvePlaceholders(
                      "http://${ports.host}:${ports.vsdmClientPort}"))
          .thenReturn(clientUrl);
      configuration
          .when(
              () ->
                  TigerGlobalConfiguration.resolvePlaceholders(
                      "http://${ports.host}:${ports.cardTerminalPort}"))
          .thenReturn(terminalUrl);
      configuration
          .when(() -> TigerGlobalConfiguration.resolvePlaceholders("${tiger.rootFolder|.}"))
          .thenReturn(directory.toString());

      steps.configureTerminalAtVsdmClient("ws://card-terminal-client");
      steps.loadCardInSlot("card.xml", 1);

      server.verify();
    }
  }
}
