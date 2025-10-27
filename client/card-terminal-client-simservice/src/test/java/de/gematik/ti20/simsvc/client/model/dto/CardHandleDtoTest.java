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
package de.gematik.ti20.simsvc.client.model.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardHandleDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    CardHandleDto dto = new CardHandleDto();
    dto.setCardHandle("HANDLE123");
    dto.setCardType("HBA");
    dto.setSlotId(2);
    dto.setCardLabel("Arztkarte");

    assertEquals("HANDLE123", dto.getCardHandle());
    assertEquals("HBA", dto.getCardType());
    assertEquals(2, dto.getSlotId());
    assertEquals("Arztkarte", dto.getCardLabel());
  }

  @Test
  void testAllArgsConstructor() {
    CardHandleDto dto = new CardHandleDto("HANDLE456", "EGK", 1, "Versichertenkarte");

    assertEquals("HANDLE456", dto.getCardHandle());
    assertEquals("EGK", dto.getCardType());
    assertEquals(1, dto.getSlotId());
    assertEquals("Versichertenkarte", dto.getCardLabel());
  }
}
