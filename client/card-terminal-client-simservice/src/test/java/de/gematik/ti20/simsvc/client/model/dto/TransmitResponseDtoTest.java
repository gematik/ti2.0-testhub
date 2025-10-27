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

class TransmitResponseDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    TransmitResponseDto dto = new TransmitResponseDto();
    dto.setResponse("90 00");
    dto.setStatusWord("9000");
    dto.setStatusMessage("Success");
    dto.setData("DEADBEEF");

    assertEquals("90 00", dto.getResponse());
    assertEquals("9000", dto.getStatusWord());
    assertEquals("Success", dto.getStatusMessage());
    assertEquals("DEADBEEF", dto.getData());
  }

  @Test
  void testConstructorWithResponseAndStatusWord() {
    TransmitResponseDto dto = new TransmitResponseDto("6A 82", "6A82");
    assertEquals("6A 82", dto.getResponse());
    assertEquals("6A82", dto.getStatusWord());
    assertNull(dto.getStatusMessage());
    assertNull(dto.getData());
  }

  @Test
  void testAllArgsConstructor() {
    TransmitResponseDto dto = new TransmitResponseDto("61 10 90 00", "9000", "OK", "6110");
    assertEquals("61 10 90 00", dto.getResponse());
    assertEquals("9000", dto.getStatusWord());
    assertEquals("OK", dto.getStatusMessage());
    assertEquals("6110", dto.getData());
  }
}
