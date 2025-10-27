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

class TransmitRequestDtoTest {

  @Test
  void testDefaultConstructorAndSetter() {
    TransmitRequestDto dto = new TransmitRequestDto();
    assertNull(dto.getCommand());
    dto.setCommand("00 A4 04 00");
    assertEquals("00 A4 04 00", dto.getCommand());
  }

  @Test
  void testConstructorWithCommand() {
    TransmitRequestDto dto = new TransmitRequestDto("80 CA 9F 7F 00");
    assertEquals("80 CA 9F 7F 00", dto.getCommand());
  }
}
