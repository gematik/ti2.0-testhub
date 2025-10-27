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

class ApduCommandTest {

  @Test
  void testConstructorAndGetters() {
    byte[] data = new byte[] {0x11, 0x22};
    ApduCommand cmd =
        new ApduCommand((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, data, 0x10);

    assertEquals((byte) 0x00, cmd.getCla());
    assertEquals((byte) 0xA4, cmd.getIns());
    assertEquals((byte) 0x04, cmd.getP1());
    assertEquals((byte) 0x00, cmd.getP2());
    assertArrayEquals(data, cmd.getData());
    assertEquals(0x10, cmd.getLe());
  }

  @Test
  void testFromHex_HeaderOnly() {
    ApduCommand cmd = ApduCommand.fromHex("00A40400");
    assertEquals((byte) 0x00, cmd.getCla());
    assertEquals((byte) 0xA4, cmd.getIns());
    assertEquals((byte) 0x04, cmd.getP1());
    assertEquals((byte) 0x00, cmd.getP2());
    assertNull(cmd.getData());
    assertNull(cmd.getLe());
  }

  @Test
  void testFromHex_WithLe() {
    ApduCommand cmd = ApduCommand.fromHex("00A4040008");
    assertEquals(8, cmd.getLe());
    assertNull(cmd.getData());
  }

  @Test
  void testFromHex_WithDataAndLe() {
    ApduCommand cmd = ApduCommand.fromHex("00A4040002AABB10");
    assertArrayEquals(new byte[] {(byte) 0xAA, (byte) 0xBB}, cmd.getData());
    assertEquals(0x10, cmd.getLe());
  }

  @Test
  void testToBytesAndToHexString() {
    ApduCommand cmd = ApduCommand.fromHex("00A4040002AABB10");
    byte[] bytes = cmd.toBytes();
    assertEquals(8, bytes.length);
    assertEquals("00A4040002AABB10", cmd.toHexString());
  }

  @Test
  void testToString() {
    ApduCommand cmd = ApduCommand.fromHex("00A4040002AABB10");
    String str = cmd.toString();
    assertTrue(str.contains("ApduCommand:"));
    assertTrue(str.contains("A4 04 00"));
    assertTrue(str.contains("[AABB]"));
    assertTrue(str.contains("10"));
  }

  @Test
  void testFromHex_InvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> ApduCommand.fromHex(""));
    assertThrows(IllegalArgumentException.class, () -> ApduCommand.fromHex("00A4"));
    assertThrows(IllegalArgumentException.class, () -> ApduCommand.fromHex("ZZZZZZZZ"));
  }
}
