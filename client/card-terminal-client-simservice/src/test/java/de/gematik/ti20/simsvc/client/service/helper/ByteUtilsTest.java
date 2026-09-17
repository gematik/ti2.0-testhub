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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ByteUtilsTest {

  @Test
  void getByteArrayReturnsEmptyArrayForNull() {
    assertArrayEquals(new byte[0], ByteUtils.getByteArray(null));
  }

  @Test
  void getByteArrayConvertsHexString() {
    assertArrayEquals(new byte[] {(byte) 0x0A, (byte) 0xFF}, ByteUtils.getByteArray("0a ff"));
  }

  @Test
  void getIntValueConvertsSingleByte() {
    assertEquals(127, ByteUtils.getIntValue(new byte[] {(byte) 0x7F}));
  }

  @Test
  void getIntValueConvertsMultipleBytes() {
    assertEquals(258, ByteUtils.getIntValue(new byte[] {(byte) 0x01, (byte) 0x02}));
    assertEquals(65536, ByteUtils.getIntValue(new byte[] {(byte) 0x01, (byte) 0x00, (byte) 0x00}));
    assertEquals(
        16909060,
        ByteUtils.getIntValue(new byte[] {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04}));
  }

  @Test
  void getIntValueRejectsNull() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.getIntValue(null));

    assertEquals("Parameter 'data' cannot be null.", exception.getMessage());
  }

  @Test
  void getIntValueRejectsEmptyArray() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.getIntValue(new byte[0]));

    assertEquals(
        "Incorrect length of parameter 'data' [Expected=1..4,Found=0].", exception.getMessage());
  }

  @Test
  void getIntValueRejectsMoreThanFourBytes() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ByteUtils.getIntValue(new byte[] {1, 2, 3, 4, 5}));

    assertEquals(
        "Incorrect length of parameter 'data' [Expected=1..4,Found=5].", exception.getMessage());
  }

  @Test
  void getIntValueRejectsNegativeIntOverflow() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ByteUtils.getIntValue(new byte[] {(byte) 0x80, 0x00, 0x00, 0x00}));

    assertEquals("Byte array value too big for datatype 'int'.", exception.getMessage());
  }
}
