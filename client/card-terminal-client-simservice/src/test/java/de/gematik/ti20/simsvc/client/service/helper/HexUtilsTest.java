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
package de.gematik.ti20.simsvc.client.service.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HexUtilsTest {

  @Test
  void decodeHexBytesDecodesValidHexWithSpaces() {
    assertArrayEquals(new byte[] {(byte) 0x0A, (byte) 0xFF}, HexUtils.decodeHexBytes("0A FF"));
  }

  @Test
  void decodeHexBytesReturnsEmptyArrayForInvalidHex() {
    assertArrayEquals(new byte[0], HexUtils.decodeHexBytes("xyz"));
  }

  @Test
  void formatHexStringReturnsNullForNullInput() {
    assertEquals(null, HexUtils.formatHexString(null, false));
  }

  @Test
  void formatHexStringUppercasesAndRemovesSpaces() {
    assertEquals("0AFF", HexUtils.formatHexString("0a ff", false));
  }

  @Test
  void formatHexStringPadsOddLengthValues() {
    assertEquals("0ABC", HexUtils.formatHexString("abc", false));
  }

  @Test
  void formatHexStringInsertsSpacesWhenRequested() {
    assertEquals("0A FF 01", HexUtils.formatHexString("0aff01", true));
  }
}
