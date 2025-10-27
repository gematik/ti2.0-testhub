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

class SmcBInfoDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    SmcBInfoDto dto = new SmcBInfoDto();
    dto.setTelematikId("1-2-3-4-5");
    dto.setProfessionOid("1.2.276.0.76.4.54");
    dto.setHolderName("Dr. Max Mustermann");
    dto.setOrganizationName("Musterklinik");
    dto.setCardType("SMC-B");

    assertEquals("1-2-3-4-5", dto.getTelematikId());
    assertEquals("1.2.276.0.76.4.54", dto.getProfessionOid());
    assertEquals("Dr. Max Mustermann", dto.getHolderName());
    assertEquals("Musterklinik", dto.getOrganizationName());
    assertEquals("SMC-B", dto.getCardType());
  }

  @Test
  void testAllArgsConstructor() {
    SmcBInfoDto dto =
        new SmcBInfoDto(
            "9-8-7-6-5", "1.2.276.0.76.4.55", "Dr. Erika Musterfrau", "Testpraxis", "SMC-B");

    assertEquals("9-8-7-6-5", dto.getTelematikId());
    assertEquals("1.2.276.0.76.4.55", dto.getProfessionOid());
    assertEquals("Dr. Erika Musterfrau", dto.getHolderName());
    assertEquals("Testpraxis", dto.getOrganizationName());
    assertEquals("SMC-B", dto.getCardType());
  }
}
