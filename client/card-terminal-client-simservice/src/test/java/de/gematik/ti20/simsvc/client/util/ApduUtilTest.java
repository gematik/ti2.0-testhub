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
package de.gematik.ti20.simsvc.client.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApduUtilTest {

  @Test
  void testHexToBytes_Valid() {
    byte[] result = ApduUtil.hexToBytes("00A4040C");
    assertArrayEquals(new byte[] {0x00, (byte) 0xA4, 0x04, 0x0C}, result);

    result = ApduUtil.hexToBytes("00 A4 04 0C");
    assertArrayEquals(new byte[] {0x00, (byte) 0xA4, 0x04, 0x0C}, result);
  }

  @Test
  void testHexToBytes_EmptyOrNull() {
    assertArrayEquals(new byte[0], ApduUtil.hexToBytes(""));
    assertArrayEquals(new byte[0], ApduUtil.hexToBytes(null));
  }

  @Test
  void testBytesToHex() {
    assertEquals("00A4040C", ApduUtil.bytesToHex(new byte[] {0x00, (byte) 0xA4, 0x04, 0x0C}));
    assertEquals("", ApduUtil.bytesToHex(null));
    assertEquals("", ApduUtil.bytesToHex(new byte[0]));
  }

  @Test
  void testExtractStatusWord() {
    byte[] response = new byte[] {0x01, 0x02, (byte) 0x90, 0x00};
    assertEquals(0x9000, ApduUtil.extractStatusWord(response));
  }

  @Test
  void testExtractStatusWord_Invalid() {
    assertThrows(IllegalArgumentException.class, () -> ApduUtil.extractStatusWord(null));
    assertThrows(IllegalArgumentException.class, () -> ApduUtil.extractStatusWord(new byte[1]));
  }

  @Test
  void testExtractResponseData() {
    byte[] response = new byte[] {0x11, 0x22, 0x33, (byte) 0x90, 0x00};
    assertArrayEquals(new byte[] {0x11, 0x22, 0x33}, ApduUtil.extractResponseData(response));
    assertArrayEquals(new byte[0], ApduUtil.extractResponseData(new byte[] {(byte) 0x90, 0x00}));
  }

  @Test
  void testExtractResponseData_Invalid() {
    assertThrows(IllegalArgumentException.class, () -> ApduUtil.extractResponseData(null));
    assertThrows(IllegalArgumentException.class, () -> ApduUtil.extractResponseData(new byte[1]));
  }

  @Test
  void testFormatApduCommand() {
    assertEquals(
        "CLA: 00, INS: A4, P1: 04, P2: 00, Le: 0C",
        ApduUtil.formatApduCommand(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x0C}));

    assertEquals(
        "CLA: 00, INS: A4, P1: 04, P2: 00",
        ApduUtil.formatApduCommand(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00}));

    // Mit Lc und Daten
    assertEquals(
        "CLA: 00, INS: A4, P1: 04, P2: 00, Lc: 02, Data: 0102",
        ApduUtil.formatApduCommand(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x02, 0x01, 0x02}));

    // Mit Lc, Daten und Le
    assertEquals(
        "CLA: 00, INS: A4, P1: 04, P2: 00, Lc: 01, Data: 01, Le: 0C",
        ApduUtil.formatApduCommand(new byte[] {0x00, (byte) 0xA4, 0x04, 0x00, 0x01, 0x01, 0x0C}));

    // Ungültig
    assertEquals("Invalid APDU command", ApduUtil.formatApduCommand(new byte[] {0x00, 0x01, 0x02}));
    assertEquals("Invalid APDU command", ApduUtil.formatApduCommand(null));
  }
}
