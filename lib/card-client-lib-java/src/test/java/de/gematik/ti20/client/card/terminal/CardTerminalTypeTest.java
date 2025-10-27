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
package de.gematik.ti20.client.card.terminal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardTerminalTypeTest {

  @Test
  void testEnumValues() {
    assertEquals("Connector", CardTerminalType.CONNECTOR.getDescription());
    assertEquals("Standard PC/SC Card Terminal", CardTerminalType.PCSC.getDescription());
    assertEquals("Card Terminal Simulator Service", CardTerminalType.SIMSVC.getDescription());
    assertEquals("Unknown Card Terminal", CardTerminalType.UNKNOWN.getDescription());
  }

  @Test
  void testValueOf() {
    assertEquals(CardTerminalType.CONNECTOR, CardTerminalType.valueOf("CONNECTOR"));
    assertEquals(CardTerminalType.PCSC, CardTerminalType.valueOf("PCSC"));
    assertEquals(CardTerminalType.SIMSVC, CardTerminalType.valueOf("SIMSVC"));
    assertEquals(CardTerminalType.UNKNOWN, CardTerminalType.valueOf("UNKNOWN"));
  }

  @Test
  void testValuesArray() {
    CardTerminalType[] values = CardTerminalType.values();
    assertEquals(4, values.length);
    assertArrayEquals(
        new CardTerminalType[] {
          CardTerminalType.CONNECTOR,
          CardTerminalType.PCSC,
          CardTerminalType.SIMSVC,
          CardTerminalType.UNKNOWN
        },
        values);
  }
}
