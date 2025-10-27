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

class SignResponseDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    SignResponseDto dto = new SignResponseDto();
    dto.setSignature("c2lnbmF0dXJl");
    dto.setAlgorithm("RS256");
    dto.setCertificate("Y2VydA==");

    assertEquals("c2lnbmF0dXJl", dto.getSignature());
    assertEquals("RS256", dto.getAlgorithm());
    assertEquals("Y2VydA==", dto.getCertificate());
  }

  @Test
  void testConstructorWithSignature() {
    SignResponseDto dto = new SignResponseDto("c2lnbmF0dXJl");
    assertEquals("c2lnbmF0dXJl", dto.getSignature());
    assertNull(dto.getAlgorithm());
    assertNull(dto.getCertificate());
  }

  @Test
  void testAllArgsConstructor() {
    SignResponseDto dto = new SignResponseDto("c2lnbmF0dXJl", "ES256", "Y2VydGlmaWNhdGU=");
    assertEquals("c2lnbmF0dXJl", dto.getSignature());
    assertEquals("ES256", dto.getAlgorithm());
    assertEquals("Y2VydGlmaWNhdGU=", dto.getCertificate());
  }
}
