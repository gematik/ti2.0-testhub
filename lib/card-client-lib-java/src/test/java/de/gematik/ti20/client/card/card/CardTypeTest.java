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
package de.gematik.ti20.client.card.card;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardTypeTest {

  @Test
  void testGetDescription() {
    assertEquals("EGK", CardType.EGK.getDescription());
    assertEquals("HBA", CardType.HBA.getDescription());
    assertEquals("SMC-B", CardType.SMC_B.getDescription());
    assertEquals("UNKNOWN", CardType.UNKNOWN.getDescription());
  }

  @Test
  void testCanSign() {
    assertFalse(CardType.EGK.canSign());
    assertTrue(CardType.HBA.canSign());
    assertTrue(CardType.SMC_B.canSign());
    assertFalse(CardType.UNKNOWN.canSign());
  }

  @Test
  void testIsSet() {
    assertFalse(CardType.isSet(null));
    assertFalse(CardType.isSet(CardType.UNKNOWN));
    assertTrue(CardType.isSet(CardType.EGK));
    assertTrue(CardType.isSet(CardType.HBA));
    assertTrue(CardType.isSet(CardType.SMC_B));
  }
}
