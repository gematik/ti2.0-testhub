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
package de.gematik.ti20.client.popp.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoppClientConfigTest {

  @Test
  void testConstructorWithTwoParams() {
    PoppClientConfig config = new PoppClientConfig("ws://real", "http://real");
    assertEquals("ws://real", config.getUrlPoppServerWs(null));
    assertEquals("http://real", config.getUrlPoppServerHttp(null));
  }

  @Test
  void testConstructorWithFourParams() {
    PoppClientConfig config =
        new PoppClientConfig("ws://real", "http://real", "ws://mock", "http://mock");
    assertEquals("ws://real", config.getUrlPoppServerWs(null));
    assertEquals("http://real", config.getUrlPoppServerHttp(null));
  }

  @Test
  void testGetUrlPoppServerHttp_withSimulatorAttachedCard() {
    PoppClientConfig config =
        new PoppClientConfig("ws://real", "http://real", "ws://mock", "http://mock");
    SimulatorAttachedCard simCard = mock(SimulatorAttachedCard.class);
    assertEquals("http://mock", config.getUrlPoppServerHttp(simCard));
  }

  @Test
  void testGetUrlPoppServerWs_withSimulatorAttachedCard() {
    PoppClientConfig config =
        new PoppClientConfig("ws://real", "http://real", "ws://mock", "http://mock");
    SimulatorAttachedCard simCard = mock(SimulatorAttachedCard.class);
    assertEquals("ws://mock", config.getUrlPoppServerWs(simCard));
  }

  @Test
  void testGetUrlPoppServerHttp_withOtherAttachedCard() {
    PoppClientConfig config =
        new PoppClientConfig("ws://real", "http://real", "ws://mock", "http://mock");
    AttachedCard card = mock(AttachedCard.class);
    assertEquals("http://real", config.getUrlPoppServerHttp(card));
  }

  @Test
  void testGetUrlPoppServerWs_withOtherAttachedCard() {
    PoppClientConfig config =
        new PoppClientConfig("ws://real", "http://real", "ws://mock", "http://mock");
    AttachedCard card = mock(AttachedCard.class);
    assertEquals("ws://real", config.getUrlPoppServerWs(card));
  }

  @Test
  void testTerminalConnectionConfigs_addAndGet() {
    PoppClientConfig config = new PoppClientConfig("ws", "http");
    CardTerminalConnectionConfig terminalConfig = mock(CardTerminalConnectionConfig.class);
    config.addTerminalConnectionConfig(terminalConfig);
    assertTrue(config.getTerminalConnectionConfigs().contains(terminalConfig));
  }

  @Test
  void testSetTerminalConnectionConfigs() {
    PoppClientConfig config = new PoppClientConfig("ws", "http");
    CardTerminalConnectionConfig c1 = mock(CardTerminalConnectionConfig.class);
    CardTerminalConnectionConfig c2 = mock(CardTerminalConnectionConfig.class);
    List<CardTerminalConnectionConfig> list = new ArrayList<>();
    list.add(c1);
    list.add(c2);
    config.setTerminalConnectionConfigs(list);
    assertEquals(2, config.getTerminalConnectionConfigs().size());
    assertTrue(config.getTerminalConnectionConfigs().containsAll(list));
  }

  @Test
  void testSetTerminalConnectionConfigs_null() {
    PoppClientConfig config = new PoppClientConfig("ws", "http");
    config.addTerminalConnectionConfig(mock(CardTerminalConnectionConfig.class));
    config.setTerminalConnectionConfigs(null);
    // Sollte unverändert bleiben
    assertEquals(1, config.getTerminalConnectionConfigs().size());
  }
}
