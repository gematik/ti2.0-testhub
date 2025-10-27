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

class ConnectionPropertiesDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    ConnectionPropertiesDto dto = new ConnectionPropertiesDto();
    dto.setCardHandle("HANDLE123");
    dto.setAtr("3B8F8001804F0CA000000306030001000000006A");
    dto.setProtocol("T=1");
    dto.setExclusive(true);

    assertEquals("HANDLE123", dto.getCardHandle());
    assertEquals("3B8F8001804F0CA000000306030001000000006A", dto.getAtr());
    assertEquals("T=1", dto.getProtocol());
    assertTrue(dto.isExclusive());
  }

  @Test
  void testAllArgsConstructor() {
    ConnectionPropertiesDto dto =
        new ConnectionPropertiesDto(
            "HANDLE456", "3B8F8001804F0CA000000306030001000000006B", "T=0", false);

    assertEquals("HANDLE456", dto.getCardHandle());
    assertEquals("3B8F8001804F0CA000000306030001000000006B", dto.getAtr());
    assertEquals("T=0", dto.getProtocol());
    assertFalse(dto.isExclusive());
  }
}
