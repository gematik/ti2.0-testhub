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

import de.gematik.ti20.client.card.card.CardType;
import org.junit.jupiter.api.Test;

class AttachedCardInfoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    AttachedCardInfo info = new AttachedCardInfo();
    assertNull(info.getId());
    assertNull(info.getType());
    assertNull(info.getSlotId());
    assertNull(info.getLabel());
  }

  @Test
  void testParameterizedConstructorAndGetters() {
    AttachedCardInfo info = new AttachedCardInfo("id123", "EGK", 1, "Testkarte");
    assertEquals("id123", info.getId());
    assertEquals("EGK", info.getType());
    assertEquals(1, info.getSlotId());
    assertEquals("Testkarte", info.getLabel());
  }

  @Test
  void testGetCardType_EGK() {
    assertEquals(CardType.EGK, new AttachedCardInfo("id", "EGK", 1, "l").getCardType());
    assertEquals(CardType.EGK, new AttachedCardInfo("id", "egkg2", 1, "l").getCardType());
    assertEquals(CardType.EGK, new AttachedCardInfo("id", "EGKG2_1", 1, "l").getCardType());
  }

  @Test
  void testGetCardType_HBA() {
    assertEquals(CardType.HBA, new AttachedCardInfo("id", "HBA", 1, "l").getCardType());
    assertEquals(CardType.HBA, new AttachedCardInfo("id", "hbag2", 1, "l").getCardType());
    assertEquals(CardType.HBA, new AttachedCardInfo("id", "HBAG2_1", 1, "l").getCardType());
  }

  @Test
  void testGetCardType_SMCB() {
    assertEquals(CardType.SMC_B, new AttachedCardInfo("id", "SMC-B", 1, "l").getCardType());
    assertEquals(CardType.SMC_B, new AttachedCardInfo("id", "smcb", 1, "l").getCardType());
    assertEquals(CardType.SMC_B, new AttachedCardInfo("id", "SMCBG2", 1, "l").getCardType());
    assertEquals(CardType.SMC_B, new AttachedCardInfo("id", "SMCBG2_1", 1, "l").getCardType());
    assertEquals(CardType.SMC_B, new AttachedCardInfo("id", "HPIC", 1, "l").getCardType());
  }

  @Test
  void testGetCardType_Unknown() {
    assertEquals(CardType.UNKNOWN, new AttachedCardInfo("id", "foo", 1, "l").getCardType());
    assertEquals(CardType.UNKNOWN, new AttachedCardInfo("id", "", 1, "l").getCardType());
  }
}
