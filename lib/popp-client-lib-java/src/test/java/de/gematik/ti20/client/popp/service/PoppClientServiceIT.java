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
package de.gematik.ti20.client.popp.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.TokenMessage;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class PoppClientServiceIT implements PoppTokenSessionEventHandler {

  // To run the integration test:
  // * Start pep und pdp with local-popp Profile
  // * Start popp-server-mockservice on port 9210
  // * Start card-terminal-client on port 8000
  //    ** insert SMC-B
  //    ** insert egk

  private CompletableFuture<TokenMessage> future = new CompletableFuture<>();

  @SneakyThrows
  @Test
  void requestPoppToken() {
    var simSvcConfig =
        new SimulatorConnectionConfig("Card Terminal SimSvc", "http://localhost:8000");

    ZetaClientConfig zetaConfig =
        new ZetaClientConfig(new ZetaClientConfig.UserAgentConfig("popp-client-lib-java", "test"));
    zetaConfig.addTerminalConnectionConfig(simSvcConfig);

    PoppClientConfig poppConfig =
        new PoppClientConfig("ws://localhost:9110", "http://localhost:9110");
    poppConfig.addTerminalConnectionConfig(simSvcConfig);

    ZetaClientService zetaClientService = new ZetaClientService(zetaConfig);

    var poppClientService = new PoppClientService(poppConfig, zetaClientService);

    List<? extends AttachedCard> cards = zetaClientService.getAttachedCards();
    assertTrue(cards != null && !cards.isEmpty(), "No SMC-B cards found in card terminal");

    cards = poppClientService.getAttachedCards();
    assertTrue(cards != null && !cards.isEmpty(), "No EGK cards found in card terminal");

    // we select the first found card for our test
    poppClientService.startPoppTokenSession(cards.stream().findFirst().get(), 0, this);

    var result = this.future.get(100, TimeUnit.SECONDS);

    log.debug("Received Popp token: {}", result.getToken());
  }

  @Override
  public void onConnectedToTerminalSlot(PoppTokenSession pts) {
    log.debug("onConnectedToTerminalSlot: {}", pts);
  }

  @Override
  public void onDisconnectedFromTerminalSlot(PoppTokenSession pts) {
    log.debug("onDisconnectedFromTerminalSlot: {}", pts);
  }

  @Override
  public void onCardInserted(PoppTokenSession pts) {
    log.debug("onCardInserted: {}", pts);
  }

  @Override
  public void onCardRemoved(PoppTokenSession pts) {
    log.debug("onCardRemoved: {}", pts);
  }

  @Override
  public void onCardPairedToServer(PoppTokenSession pts) {
    log.debug("onCardPairedToServer: {}", pts);
  }

  @Override
  public void onConnectedToServer(PoppTokenSession pts) {
    log.debug("onConnectedToServer: {}", pts);
  }

  @Override
  public void onDisconnectedFromServer(PoppTokenSession pts) {
    future.completeExceptionally(new Exception("Unexpected disconnect from server"));
  }

  @Override
  public void onError(PoppTokenSession pts, PoppClientException exception) {
    future.completeExceptionally(exception);
  }

  @Override
  public void onReceivedPoppToken(PoppTokenSession pts, TokenMessage tokenMessage) {
    future.complete(tokenMessage);
  }

  @Override
  public void onFinished(PoppTokenSession pts) {}
}
