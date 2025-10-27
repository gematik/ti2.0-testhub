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
package de.gematik.ti20.client.card.message;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.client.card.exception.CardMessageException;
import org.junit.jupiter.api.Test;

class CardMessageParserTest {

  @Test
  void testParseWithNullString() {
    CardMessage msg = CardMessageParser.parse((String) null);
    assertNotNull(msg);
    assertEquals("", msg.getText());
    assertEquals(0, msg.getData().length);
  }

  @Test
  void testParseWithString() {
    String text = "Test";
    CardMessage msg = CardMessageParser.parse(text);
    assertEquals(text, msg.getText());
  }

  @Test
  void testParseWithByteArray() {
    byte[] data = "Hallo".getBytes();
    CardMessage msg = CardMessageParser.parse(data);
    assertEquals("Hallo", msg.getText());
  }

  @Test
  void testParseCommandFromJsonValid() throws Exception {
    String json = "{\"type\":\"getCardData\"}";
    CardCommand cmd = CardMessageParser.parseCommandFromJson(json);
    assertNotNull(cmd);
    assertEquals(CardCommand.Type.GET_CARD_DATA, cmd.getType());
  }

  @Test
  void testParseCommandFromJsonInvalid() {
    String invalidJson = "{invalid}";
    assertThrows(
        CardMessageException.class, () -> CardMessageParser.parseCommandFromJson(invalidJson));
  }

  @Test
  void testParseResultFromJsonValid() throws Exception {
    String json = "{\"type\":\"result\",\"bar\":456}";
    CardResult result = CardMessageParser.parseResultFromJson(json);
    assertNotNull(result);
  }

  @Test
  void testParseResultFromJsonInvalid() {
    String invalidJson = "{invalid}";
    assertThrows(
        CardMessageException.class, () -> CardMessageParser.parseResultFromJson(invalidJson));
  }
}
