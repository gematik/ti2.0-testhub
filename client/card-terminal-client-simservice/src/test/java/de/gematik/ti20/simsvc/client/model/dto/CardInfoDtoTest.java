/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.model.dto;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardInfoDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    CardInfoDto dto = new CardInfoDto();
    dto.setCardId("card-id");
    dto.setCardType("HBA");
    dto.setSlotId(2);
    dto.setLabel("Arztkarte");

    assertEquals("card-id", dto.getCardId());
    assertEquals("HBA", dto.getCardType());
    assertEquals(2, dto.getSlotId());
    assertEquals("Arztkarte", dto.getLabel());
  }

  @Test
  void testAllArgsConstructor() {
    CardInfoDto dto = new CardInfoDto("card-id", "EGK", 1, "Versichertenkarte");

    assertEquals("card-id", dto.getCardId());
    assertEquals("EGK", dto.getCardType());
    assertEquals(1, dto.getSlotId());
    assertEquals("Versichertenkarte", dto.getLabel());
  }

  @Test
  void testFromCreatesDtoFromVirtualCardAndEgkInfo() {
    VirtualCardImageData virtualCard = new VirtualCardImageData(Map.of("EF.PD", "data"));
    CardInfoDto virtualDto = CardInfoDto.from(virtualCard, 7);

    assertNotNull(virtualDto);
    assertEquals("EGK", virtualDto.getCardType());
    assertEquals(7, virtualDto.getSlotId());
    assertEquals("EGK", virtualDto.getLabel());

    EgkInfoDto egkInfo = new EgkInfoDto();
    egkInfo.setKvnr("X123456789");

    CardInfoDto egkDto = CardInfoDto.from(egkInfo, 3);

    assertNotNull(egkDto);
    assertEquals("EGK", egkDto.getCardType());
    assertEquals(3, egkDto.getSlotId());
    assertEquals("egk-X123456789", egkDto.getLabel());
  }
}
