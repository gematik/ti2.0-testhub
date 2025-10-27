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

class StartMessageTest {

  @Test
  void testDefaultConstructor() {
    StartMessage msg = new StartMessage();
    assertEquals(BasePoppMessageType.START.getValue(), msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertNull(msg.getCardConnectionType());
    assertNull(msg.getClientSessionId());
  }

  @Test
  void testConstructorWithParams() {
    StartMessage msg = new StartMessage("CT", "session-1");
    assertEquals(BasePoppMessageType.START.getValue(), msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertEquals("CT", msg.getCardConnectionType());
    assertEquals("session-1", msg.getClientSessionId());
  }

  @Test
  void testConstructorWithCardConnectionType() {
    StartMessage msg = new StartMessage("PCSC");
    assertEquals(BasePoppMessageType.START.getValue(), msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertEquals("PCSC", msg.getCardConnectionType());
    assertNotNull(msg.getClientSessionId());
    // UUID-Format grob prüfen
    assertTrue(msg.getClientSessionId().matches("^[0-9a-fA-F\\-]{36}$"));
  }

  @Test
  void testSettersAndGetters() {
    StartMessage msg = new StartMessage();
    msg.setVersion("2.0.0");
    msg.setCardConnectionType("USB");
    msg.setClientSessionId("abc-123");
    assertEquals("2.0.0", msg.getVersion());
    assertEquals("USB", msg.getCardConnectionType());
    assertEquals("abc-123", msg.getClientSessionId());
  }

  @Test
  void testJacksonSerializationAndDeserialization() throws Exception {
    StartMessage msg = new StartMessage("CT", "sess-42");
    msg.setVersion("3.1.4");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(msg);
    assertTrue(json.contains("\"version\":\"3.1.4\""));
    assertTrue(json.contains("\"cardConnectionType\":\"CT\""));
    assertTrue(json.contains("\"clientSessionId\":\"sess-42\""));
    assertTrue(json.contains("\"type\":\"" + BasePoppMessageType.START.getValue() + "\""));

    StartMessage deserialized = mapper.readValue(json, StartMessage.class);
    assertEquals("3.1.4", deserialized.getVersion());
    assertEquals("CT", deserialized.getCardConnectionType());
    assertEquals("sess-42", deserialized.getClientSessionId());
    assertEquals(BasePoppMessageType.START.getValue(), deserialized.getType());
  }
}
