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
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BasePoppMessageTest {

  @Test
  void testConstructorWithEnum() {
    BasePoppMessageType type = mock(BasePoppMessageType.class);
    when(type.getValue()).thenReturn("testType");
    BasePoppMessage msg = new BasePoppMessage(type);
    assertEquals("testType", msg.getType());
  }

  @Test
  void testConstructorWithString() {
    BasePoppMessage msg = new BasePoppMessage("myType");
    assertEquals("myType", msg.getType());
  }

  @Test
  void testSetType() {
    BasePoppMessage msg = new BasePoppMessage("init");
    msg.setType("changed");
    assertEquals("changed", msg.getType());
  }

  @Test
  void testJacksonDeserialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = "{\"type\":\"jsonType\"}";
    BasePoppMessage msg = mapper.readValue(json, BasePoppMessage.class);
    assertEquals("jsonType", msg.getType());
  }
}
