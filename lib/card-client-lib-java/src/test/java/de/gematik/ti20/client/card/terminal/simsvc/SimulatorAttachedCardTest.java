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

import de.gematik.ti20.client.card.card.CardType;
import org.junit.jupiter.api.Test;

class SimulatorAttachedCardTest {

  @Test
  void testConstructorAndGetters() {
    AttachedCardInfo info = new AttachedCardInfo("id123", "EGK", 2, "Testkarte");
    SimulatorCardTerminal terminal = mock(SimulatorCardTerminal.class);

    SimulatorAttachedCard card = new SimulatorAttachedCard(info, terminal);

    assertEquals("id123", card.getId());
    assertEquals(CardType.EGK, card.getType());
    assertEquals(2, card.getSlotId());
    assertEquals("Testkarte", card.getLabel());
    assertEquals(terminal, card.getTerminal());
  }

  @Test
  void testGetInfo() {
    AttachedCardInfo info = new AttachedCardInfo("id456", "HBA", 3, "Label");
    SimulatorCardTerminal terminal = mock(SimulatorCardTerminal.class);

    SimulatorAttachedCard card = new SimulatorAttachedCard(info, terminal);

    String expected = "Type: HBA, Simulator ID: id456";
    assertEquals(expected, card.getInfo());
  }
}
