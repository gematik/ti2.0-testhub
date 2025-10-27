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
package de.gematik.ti20.simsvc.client.model.apdu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApduResponseTest {

  @Test
  void testConstructorAndGetters() {
    byte[] data = new byte[] {0x01, 0x02, 0x03};
    ApduResponse resp = new ApduResponse(data, (byte) 0x90, (byte) 0x00);

    assertArrayEquals(data, resp.getData());
    assertEquals((byte) 0x90, resp.getSw1());
    assertEquals((byte) 0x00, resp.getSw2());
    assertEquals(0x9000, resp.getStatusWord());
    assertEquals("9000", resp.getStatusWordHex());
    assertEquals("Success", resp.getStatusMessage());
    assertTrue(resp.isSuccess());
  }

  @Test
  void testConstructorStatusOnly() {
    ApduResponse resp = new ApduResponse((byte) 0x6A, (byte) 0x82);
    assertNull(resp.getData());
    assertEquals(0x6A82, resp.getStatusWord());
    assertEquals("Error: File not found", resp.getStatusMessage());
    assertFalse(resp.isSuccess());
  }

  @Test
  void testConstructorIntStatusWord() {
    ApduResponse resp = new ApduResponse(0x6A84);
    assertEquals((byte) 0x6A, resp.getSw1());
    assertEquals((byte) 0x84, resp.getSw2());
    assertEquals("Error: Not enough memory space in the file", resp.getStatusMessage());
  }

  @Test
  void testCreateSuccessResponse() {
    ApduResponse resp = ApduResponse.createSuccessResponse();
    assertEquals(0x9000, resp.getStatusWord());
    assertTrue(resp.isSuccess());
  }

  @Test
  void testCreateErrorResponse() {
    ApduResponse resp = ApduResponse.createErrorResponse(0x6D00);
    assertEquals("Error: Instruction code not supported or invalid", resp.getStatusMessage());
    assertFalse(resp.isSuccess());
  }

  @Test
  void testToBytesAndToHex() {
    byte[] data = new byte[] {0x0A, 0x0B};
    ApduResponse resp = new ApduResponse(data, (byte) 0x90, (byte) 0x00);
    byte[] bytes = resp.toBytes();
    assertEquals(4, bytes.length);
    assertEquals(0x0A, bytes[0]);
    assertEquals(0x0B, bytes[1]);
    assertEquals((byte) 0x90, bytes[2]);
    assertEquals((byte) 0x00, bytes[3]);
    assertEquals("0A0B9000", resp.toHex());
  }

  @Test
  void testToString() {
    ApduResponse resp = new ApduResponse(new byte[] {0x01, 0x02}, (byte) 0x6A, (byte) 0x82);
    String str = resp.toString();
    assertTrue(str.contains("ApduResponse:"));
    assertTrue(str.contains("[0102]"));
    assertTrue(str.contains("6A82"));
    assertTrue(str.contains("File not found"));
  }

  @Test
  void testUnknownStatusWord() {
    ApduResponse resp = new ApduResponse((byte) 0x12, (byte) 0x34);
    assertTrue(resp.getStatusMessage().startsWith("Unknown status word:"));
  }
}
