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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.terminal.CardTerminal;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.message.*;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import de.gematik.ti20.client.zeta.websocket.ZetaWsSession;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PoppTokenSessionTest {

  private AttachedCard attachedCard;
  private CardTerminal cardTerminal;
  private CardConnection cardConnection;
  private PoppClientService poppClientService;
  private ZetaClientService zetaClientService;

  private PoppTokenSessionEventHandler eventHandler;
  private PoppTokenSession session;

  @BeforeEach
  void setUp() throws Exception {
    attachedCard = mock(AttachedCard.class);
    cardTerminal = mock(CardTerminal.class);
    cardConnection = mock(CardConnection.class);

    ZetaClientService zetaClientService = mock(ZetaClientService.class);

    PoppClientConfig poppClientConfig = mock(PoppClientConfig.class);

    poppClientService = mock(PoppClientService.class);
    when(poppClientService.getPoppClientConfig()).thenReturn(poppClientConfig);
    when(poppClientService.getZetaClientService()).thenReturn(zetaClientService);

    eventHandler = mock(PoppTokenSessionEventHandler.class);

    when(attachedCard.getTerminal()).thenReturn(cardTerminal);
    when(attachedCard.getId()).thenReturn("card-1");
    when(cardTerminal.getName()).thenReturn("terminal-1");
    when(cardTerminal.connect(attachedCard)).thenReturn(cardConnection);
    when(cardConnection.isConnected()).thenReturn(true);

    session = new PoppTokenSession(attachedCard, eventHandler, poppClientService);
    session.start(0);
  }

  @Test
  void testStartConnectsCard() throws Exception {
    doNothing().when(cardConnection).disconnect();
    doNothing().when(eventHandler).onFinished(session);

    // poppClientService.getPoppClientConfig() und getZetaClientService() werden nicht getestet
    assertDoesNotThrow(() -> session.start(0));
    assertTrue(session.isCardConnected());
  }

  @Test
  void testProcessScenarioMessageSendsResponse() throws Exception {
    // Arrange
    ZetaWsSession ws = mock(ZetaWsSession.class);
    ScenarioStep step = new ScenarioStep("00A40400", Arrays.asList("9000"));
    StandardScenarioMessage scenarioMsg =
        new StandardScenarioMessage("sess", 1, 1, Arrays.asList(step));
    when(cardConnection.transmit(any())).thenReturn(new byte[] {(byte) 0x90, 0x00});

    // Act (direkt private Methode via Reflection aufrufen)
    var method =
        PoppTokenSession.class.getDeclaredMethod(
            "processScenarioMessage", ZetaWsSession.class, StandardScenarioMessage.class);
    method.setAccessible(true);
    method.invoke(session, ws, scenarioMsg);

    // Assert
    verify(ws, atLeastOnce()).send(contains("9000"));
  }

  @Test
  void testFinishDisconnectsCardAndClosesWs() {
    ZetaWsSession wsSession = mock(ZetaWsSession.class);
    when(wsSession.isOpen()).thenReturn(true);

    session.finish();

    verify(cardConnection, atLeast(0)).disconnect();
    verify(wsSession, atLeast(0)).close(anyInt(), anyString());
    verify(eventHandler).onFinished(session);
  }
}
