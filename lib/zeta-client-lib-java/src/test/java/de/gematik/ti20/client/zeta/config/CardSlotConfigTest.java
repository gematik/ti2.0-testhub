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
package de.gematik.ti20.client.zeta.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardSlotConfigTest {

  @Test
  void testConstructorAndGetters() {
    CardSlotConfig.CardSlotType type = CardSlotConfig.CardSlotType.SMCB;
    CardSlotConfig config = new CardSlotConfig("TestName", type, "http://test.url", "slot-1");

    assertEquals("TestName", config.getName());
    assertEquals(type, config.getType());
    assertEquals("http://test.url", config.getUrl());
    assertEquals("slot-1", config.getSlotId());
  }

  @Test
  void testCardSlotTypeEnumValues() {
    assertArrayEquals(
        new CardSlotConfig.CardSlotType[] {
          CardSlotConfig.CardSlotType.SIM,
          CardSlotConfig.CardSlotType.SMCB,
          CardSlotConfig.CardSlotType.SMB
        },
        CardSlotConfig.CardSlotType.values());
  }
}
