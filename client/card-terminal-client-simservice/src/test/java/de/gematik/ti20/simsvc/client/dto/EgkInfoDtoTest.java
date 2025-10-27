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
package de.gematik.ti20.simsvc.client.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EgkInfoDtoTest {

  @Test
  void testDefaultCardType() {
    EgkInfoDto dto = new EgkInfoDto();
    assertEquals("EGK", dto.getCardType());
  }

  @Test
  void testSettersAndGetters() {
    EgkInfoDto dto = new EgkInfoDto();

    dto.setKvnr("kvnr123");
    dto.setIknr("iknr456");
    dto.setPatientName("Max Mustermann");
    dto.setFirstName("Max");
    dto.setLastName("Mustermann");
    dto.setDateOfBirth("1990-01-01");
    dto.setInsuranceName("AOK");
    dto.setCardType("EGK2");
    dto.setValidUntil("2030-12-31");

    assertEquals("kvnr123", dto.getKvnr());
    assertEquals("iknr456", dto.getIknr());
    assertEquals("Max Mustermann", dto.getPatientName());
    assertEquals("Max", dto.getFirstName());
    assertEquals("Mustermann", dto.getLastName());
    assertEquals("1990-01-01", dto.getDateOfBirth());
    assertEquals("AOK", dto.getInsuranceName());
    assertEquals("EGK2", dto.getCardType());
    assertEquals("2030-12-31", dto.getValidUntil());
  }
}
