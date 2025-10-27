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
package de.gematik.ti20.simsvc.client.service.protocol;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import org.junit.jupiter.api.Test;

class AbstractCardProtocolTest {

  // Anonyme Unterklasse für Testzwecke
  private final AbstractCardProtocol protocol =
      new AbstractCardProtocol() {
        @Override
        public boolean canHandle(ApduCommand command) {
          return false;
        }

        @Override
        public ApduResponse processCommand(CardImage card, ApduCommand command) {
          return null;
        }

        @Override
        public void reset() {}

        @Override
        public String getProtocolName() {
          return "";
        }
      };

  @Test
  void testHexEncode() {
    assertEquals("0102ff", protocol.hexEncode(new byte[] {0x01, 0x02, (byte) 0xFF}));
    assertEquals("null", protocol.hexEncode(null));
    assertEquals("", protocol.hexEncode(new byte[0]));
  }

  @Test
  void testExtractTlvValue_FindsTag() {
    // TLV: 0x5A 0x03 0x11 0x22 0x33
    byte[] data = new byte[] {0x5A, 0x03, 0x11, 0x22, 0x33};
    byte[] value = protocol.extractTlvValue(data, (byte) 0x5A);
    assertArrayEquals(new byte[] {0x11, 0x22, 0x33}, value);
  }

  @Test
  void testExtractTlvValue_NotFoundOrInvalid() {
    // Tag nicht vorhanden
    byte[] data = new byte[] {0x5A, 0x01, 0x10};
    assertNull(protocol.extractTlvValue(data, (byte) 0x4F));

    // Zu kurzes Array
    assertNull(protocol.extractTlvValue(new byte[] {0x01}, (byte) 0x01));
    assertNull(protocol.extractTlvValue(null, (byte) 0x01));
  }

  @Test
  void testExtractTlvValue_WithOuterTag7C() {
    // 7C 05 5A 03 01 02 03
    byte[] data = new byte[] {0x7C, 0x05, 0x5A, 0x03, 0x01, 0x02, 0x03};
    byte[] value = protocol.extractTlvValue(data, (byte) 0x5A);
    assertArrayEquals(new byte[] {0x01, 0x02, 0x03}, value);
  }

  @Test
  void testCreateSuccessAndErrorResponse() {
    ApduResponse success = protocol.createSuccessResponse();
    assertNotNull(success);

    ApduResponse error = protocol.createErrorResponse(0x6A82);
    assertNotNull(error);
  }
}
