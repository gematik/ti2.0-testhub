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
package de.gematik.ti20.client.popp.message;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TokenMessageTest {

  @Test
  void testDefaultConstructor() {
    TokenMessage msg = new TokenMessage();
    assertEquals(BasePoppMessageType.TOKEN.getValue(), msg.getType());
    assertNull(msg.getToken());
    assertNull(msg.getPn());
  }

  @Test
  void testConstructorWithParams() {
    TokenMessage msg = new TokenMessage("tok123", "pn456");
    assertEquals(BasePoppMessageType.TOKEN.getValue(), msg.getType());
    assertEquals("tok123", msg.getToken());
    assertEquals("pn456", msg.getPn());
  }

  @Test
  void testSettersAndGetters() {
    TokenMessage msg = new TokenMessage();
    msg.setToken("abc");
    msg.setPn("def");
    assertEquals("abc", msg.getToken());
    assertEquals("def", msg.getPn());
  }

  @Test
  void testJacksonSerializationAndDeserialization() throws Exception {
    TokenMessage msg = new TokenMessage("t1", "pn1");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(msg);
    assertTrue(json.contains("\"token\":\"t1\""));
    assertTrue(json.contains("\"pn\":\"pn1\""));
    assertTrue(json.contains("\"type\":\"" + BasePoppMessageType.TOKEN.getValue() + "\""));

    TokenMessage deserialized = mapper.readValue(json, TokenMessage.class);
    assertEquals("t1", deserialized.getToken());
    assertEquals("pn1", deserialized.getPn());
    assertEquals(BasePoppMessageType.TOKEN.getValue(), deserialized.getType());
  }
}
