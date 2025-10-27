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
package de.gematik.ti20.client.card.terminal.pcsc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.CardType;
import javax.smartcardio.ATR;
import org.junit.jupiter.api.Test;

class PcScAttachedCardTest {

  @Test
  void testConstructorAndGetters() {
    String id = "card123";
    CardType type = CardType.EGK;
    PcScCardTerminal terminal = mock(PcScCardTerminal.class);
    byte[] atrBytes = new byte[] {0x3B, 0x13, 0x00};
    ATR atr = new ATR(atrBytes);

    PcScAttachedCard card = new PcScAttachedCard(id, type, terminal, atr);

    assertEquals(id, card.getId());
    assertEquals(type, card.getType());
    assertEquals(terminal, card.getTerminal());
    assertEquals(atr, card.getATR());
  }

  @Test
  void testGetInfo() {
    CardType type = mock(CardType.class);
    when(type.getDescription()).thenReturn("eGK");
    PcScCardTerminal terminal = mock(PcScCardTerminal.class);
    byte[] atrBytes = new byte[] {0x3B, 0x13, 0x00};
    ATR atr = new ATR(atrBytes);

    PcScAttachedCard card = new PcScAttachedCard("id", type, terminal, atr);

    String expectedATR = "3B1300";
    String expected = "Type: eGK, ATR: " + expectedATR;
    assertEquals(expected, card.getInfo());
  }
}
