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
package de.gematik.ti20.client.card.terminal.simsvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminal;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulatorCardTerminalTest {

  private SimulatorConnectionConfig config;
  private SimulatorCardTerminal terminal;

  @BeforeEach
  void setUp() {
    config = mock(SimulatorConnectionConfig.class);
    when(config.getName()).thenReturn("SimTerm");
    when(config.getUrl()).thenReturn("http://localhost:1234");

    terminal = new SimulatorCardTerminal(config);
  }

  @Test
  void testConstructor() {
    assertEquals("SimTerm", terminal.getName());
    assertEquals("SimTerm", terminal.getConfig().getName());
    assertNotNull(terminal.getClient());
  }

  @Test
  void testGetAttachedCards_success() throws Exception {
    SimulatorClient clientMock = mock(SimulatorClient.class);
    AttachedCardInfo cardInfo = new AttachedCardInfo("id", "EGK", 1, "label");
    when(clientMock.getAvailableCards()).thenReturn(List.of(cardInfo));
    SimulatorCardTerminal term =
        new SimulatorCardTerminal(config) {
          @Override
          public SimulatorClient getClient() {
            return clientMock;
          }
        };

    List<? extends AttachedCard> cards = term.getAttachedCards();
    assertEquals(1, cards.size());
    assertTrue(cards.get(0) instanceof SimulatorAttachedCard);
    assertEquals("id", cards.get(0).getId());
  }

  @Test
  void testGetAttachedCards_throwsException() throws Exception {
    SimulatorClient clientMock = mock(SimulatorClient.class);
    when(clientMock.getAvailableCards()).thenThrow(new IOException("fail"));
    SimulatorCardTerminal term =
        new SimulatorCardTerminal(config) {
          @Override
          public SimulatorClient getClient() {
            return clientMock;
          }
        };

    assertThrows(CardTerminalException.class, term::getAttachedCards);
  }

  @Test
  void testConnect_success() throws Exception {
    SimulatorClient clientMock = mock(SimulatorClient.class);
    SimulatorAttachedCard card = mock(SimulatorAttachedCard.class);
    CardTerminal terminalMock = mock(CardTerminal.class);
    when(card.getId()).thenReturn("testCard");
    when(card.getTerminal()).thenReturn(terminalMock);

    CardConnectionInfo cci = mock(CardConnectionInfo.class);
    when(clientMock.connectToCard(card)).thenReturn(cci);

    SimulatorCardTerminal term =
        new SimulatorCardTerminal(config) {
          @Override
          public SimulatorClient getClient() {
            return clientMock;
          }
        };

    CardConnection conn = term.connect(card);
    assertNotNull(conn);
    assertTrue(conn instanceof SimulatorCardConnection);
  }

  @Test
  void testConnect_wrongType() {
    AttachedCard card = mock(AttachedCard.class);
    assertThrows(CardTerminalException.class, () -> terminal.connect(card));
  }

  @Test
  void testConnect_throwsException() throws Exception {
    SimulatorClient clientMock = mock(SimulatorClient.class);
    SimulatorAttachedCard card = mock(SimulatorAttachedCard.class);
    when(clientMock.connectToCard(card)).thenThrow(new IOException("fail"));

    SimulatorCardTerminal term =
        new SimulatorCardTerminal(config) {
          @Override
          public SimulatorClient getClient() {
            return clientMock;
          }
        };

    assertThrows(CardTerminalException.class, () -> term.connect(card));
  }

  @Test
  void testGetEgkInfo_success() throws Exception {
    SimulatorClient clientMock = mock(SimulatorClient.class);
    SimulatorAttachedCard attachedCard = mock(SimulatorAttachedCard.class);
    when(attachedCard.getTerminal()).thenReturn(terminal);

    EgkInfo info = mock(EgkInfo.class);
    when(clientMock.getEgkInfo(attachedCard)).thenReturn(info);

    SimulatorCardTerminal term =
        new SimulatorCardTerminal(config) {
          @Override
          public SimulatorClient getClient() {
            return clientMock;
          }
        };

    EgkInfo result = term.getEgkInfo(attachedCard);

    assertNotNull(result);
    assertEquals(info, result);
  }

  // TODO  @Test
  void testGetEgkInfo_throwsException() throws Exception {
    SimulatorClient clientMock = mock(SimulatorClient.class);
    SimulatorAttachedCard attachedCard = mock(SimulatorAttachedCard.class);
    when(attachedCard.getTerminal()).thenReturn(terminal);

    when(clientMock.getEgkInfo(attachedCard)).thenThrow(new IOException("fail"));

    assertThrows(CardTerminalException.class, () -> terminal.getEgkInfo(attachedCard));
  }
}
