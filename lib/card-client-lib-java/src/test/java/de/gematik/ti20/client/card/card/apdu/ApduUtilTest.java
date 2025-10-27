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
package de.gematik.ti20.client.card.card.apdu;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ApduUtilTest {

  @Test
  void testCreateSelectApdu() {
    byte[] aid = {0x01, 0x02, 0x03};
    byte[] apdu = ApduUtil.createSelectApdu(aid);
    assertEquals(6 + aid.length, apdu.length);
    assertEquals((byte) 0xA4, apdu[1]);
    assertEquals((byte) aid.length, apdu[4]);
    assertArrayEquals(aid, Arrays.copyOfRange(apdu, 5, 5 + aid.length));
    assertEquals(0x00, apdu[apdu.length - 1]);
  }

  @Test
  void testBuildSelectApplicationApdu_validHex() {
    String aidHex = "01020304";
    byte[] apdu = ApduUtil.buildSelectApplicationApdu(aidHex);
    assertEquals(10, apdu.length);
    assertEquals((byte) 0xA4, apdu[1]);
  }

  @Test
  void testBuildSelectApplicationApdu_invalidHex() {
    byte[] apdu = ApduUtil.buildSelectApplicationApdu("invalid");
    // Sollte auf Default-AID zurückfallen
    assertEquals(14, apdu.length);
    assertEquals((byte) 0xA4, apdu[1]);
  }

  @Test
  void testCreateMSESetCommand_default() {
    byte[] apdu = ApduUtil.createMSESetCommand();
    assertEquals(11, apdu.length);
    assertEquals(0x22, apdu[1]);
    assertEquals(0x10, apdu[7]);
    assertEquals(0x01, apdu[9]);
  }

  @Test
  void testCreateMSESetCommand_variousKeyRefs() {
    // Dezimal
    byte[] apdu1 = ApduUtil.createMSESetCommand("2");
    assertEquals(2, apdu1[10]);
    // Hex mit Prefix
    byte[] apdu2 = ApduUtil.createMSESetCommand("0x0A");
    assertEquals(0x0A, apdu2[10]);
    // Hex ohne Prefix
    byte[] apdu3 = ApduUtil.createMSESetCommand("0F");
    assertEquals(0x0F, apdu3[10]);
    // Ungültig -> Default
    byte[] apdu4 = ApduUtil.createMSESetCommand("foo");
    assertEquals(1, apdu4[10]);
  }

  @Test
  void testCreateVerifyPINCommand() {
    byte[] pin = {1, 2, 3, 4};
    byte[] apdu = ApduUtil.createVerifyPINCommand(pin);
    assertEquals(9, apdu.length);
    assertEquals(0x20, apdu[1]);
    assertEquals(pin.length, apdu[4]);
    assertArrayEquals(pin, Arrays.copyOfRange(apdu, 5, 9));
  }

  @Test
  void testCreateSignCommand() {
    byte[] hash = new byte[32];
    Arrays.fill(hash, (byte) 0xAA);
    byte[] apdu = ApduUtil.createSignCommand(hash);
    assertEquals(37, apdu.length);
    assertEquals(0x2A, apdu[1]);
    assertEquals(hash.length, apdu[4]);
    assertArrayEquals(hash, Arrays.copyOfRange(apdu, 5, 37));
  }

  @Test
  void testBuildSignCommandApdu() {
    byte[] hash = {0x01, 0x02};
    assertArrayEquals(ApduUtil.createSignCommand(hash), ApduUtil.buildSignCommandApdu(hash));
  }

  @Test
  void testCreateHashFromDataAndHashData() throws Exception {
    byte[] data = "abc".getBytes();
    byte[] hash1 = ApduUtil.createHashFromData(data);
    byte[] hash2 = ApduUtil.hashData(data);
    assertArrayEquals(hash1, hash2);
    assertEquals(32, hash1.length); // SHA-256 Länge
  }

  @Test
  void testIsSuccess() {
    byte[] response = {0x01, 0x02, (byte) 0x90, 0x00};
    assertTrue(ApduUtil.isSuccess(response));
    assertTrue(ApduUtil.isSuccessResponse(response));
    byte[] fail = {0x01, 0x02, (byte) 0x6A, (byte) 0x82};
    assertFalse(ApduUtil.isSuccess(fail));
    assertFalse(ApduUtil.isSuccess(new byte[1]));
  }

  @Test
  void testGetResponseStatusString() {
    byte[] response = {0x01, 0x02, (byte) 0x90, 0x00};
    assertEquals("0x9000", ApduUtil.getResponseStatusString(response));
    assertEquals("0x9000", ApduUtil.getStatusString(response));
    assertEquals("Invalid response", ApduUtil.getResponseStatusString(new byte[1]));
  }

  @Test
  void testExtractDataFromResponse() {
    byte[] response = {0x11, 0x22, 0x33, 0x44, (byte) 0x90, 0x00};
    byte[] data = ApduUtil.extractDataFromResponse(response);
    assertArrayEquals(new byte[] {0x11, 0x22, 0x33, 0x44}, data);
    assertArrayEquals(data, ApduUtil.extractSignature(response));
    assertArrayEquals(new byte[0], ApduUtil.extractDataFromResponse(new byte[2]));
  }

  @Test
  void testBytesToHexAndHexToBytes() {
    byte[] bytes = {0x0A, 0x1B, (byte) 0xFF};
    String hex = ApduUtil.bytesToHex(bytes);
    assertEquals("0A1BFF", hex);
    assertArrayEquals(bytes, ApduUtil.hexToBytes(hex));
  }
}
