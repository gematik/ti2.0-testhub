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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardCommandTest {

  @Test
  void testTypeEnumJson() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(CardCommand.Type.GET_CARD_DATA);
    assertEquals("\"getCardData\"", json);

    CardCommand.Type type = mapper.readValue("\"sign\"", CardCommand.Type.class);
    assertEquals(CardCommand.Type.SIGN, type);
  }

  @Test
  void testConstructorAndGetters() {
    CardCommand cmd = new CardCommand(CardCommand.Type.SIGN);
    assertEquals(CardCommand.Type.SIGN, cmd.getType());
    assertTrue(cmd.getArgs().isEmpty());
  }

  @Test
  void testAddAndGetArg() {
    CardCommand cmd = new CardCommand(CardCommand.Type.GET_CARD_DATA);
    cmd.addArg("foo", "bar");
    assertEquals("bar", cmd.getArg("foo"));

    cmd.addArg("num", 42);
    assertEquals("42", cmd.getArg("num"));
  }

  @Test
  void testJsonSerialization() throws Exception {
    CardCommand cmd = new CardCommand(CardCommand.Type.GET_CARD_DATA);
    cmd.addArg("a", "b");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(cmd);

    assertTrue(json.contains("\"type\":\"getCardData\""));
    assertTrue(json.contains("\"args\":{\"a\":\"b\"}"));

    CardCommand deserialized = mapper.readValue(json, CardCommand.class);
    assertEquals(CardCommand.Type.GET_CARD_DATA, deserialized.getType());
    assertEquals("b", deserialized.getArg("a"));
  }

  @Test
  void testConstructorWithArgs() {
    Map<String, String> args = Map.of("x", "y");
    CardCommand cmd = new CardCommand(CardCommand.Type.SIGN, args);
    assertEquals("y", cmd.getArg("x"));
  }
}
