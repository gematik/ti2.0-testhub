/*
 *
 * Copyright 2025 gematik GmbH
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
package de.gematik.ti20.client.zeta.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig.UserAgentConfig;
import de.gematik.ti20.client.zeta.request.ZetaWsRequest;
import de.gematik.ti20.client.zeta.websocket.ZetaWsEventHandler;
import de.gematik.ti20.client.zeta.websocket.ZetaWsSession;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ZetaClientServiceIT {

  // To run this integration test, you need to have the following services running:
  // - zeta-pep-server-mockservice and zeta-pdp-server-mockservice: pdp with local-test Profile
  // - zeta-test-server-simservice
  // - card-terminal-client
  //   - running on port 8000
  //   - inserted SMC-B
  @SneakyThrows
  @Test
  void testGetByWs() {
    ZetaClientConfig zetaConfig =
        new ZetaClientConfig(new UserAgentConfig("zeta-client-lib-java", "test"));
    zetaConfig.addTerminalConnectionConfig(
        new SimulatorConnectionConfig("Card Terminal SimSvc", "http://localhost:8000"));

    ZetaClientService zetaClientService = new ZetaClientService(zetaConfig);
    assertNotNull(zetaClientService);
    var cards = zetaClientService.getAttachedCards();
    assertTrue(cards != null && !cards.isEmpty(), "No SMC-B cards found in card terminal");

    var request = new ZetaWsRequest("ws://localhost:9100/ws/tapi/return", "http://localhost:9100");

    CompletableFuture<ZetaWsSession> resultFuture = new CompletableFuture<>();

    var eventHandler = new ZetaWsEventHandler(resultFuture);
    try {
      zetaClientService.connectToPepProxy(request, eventHandler, true);
      ZetaWsSession ws = resultFuture.get(10, TimeUnit.SECONDS);
      log.debug("Received ws response: {}", ws.getLastMessage());
    } catch (Exception e) {
      log.error("Error in requestTestWsGet", e);
    }
  }
}
