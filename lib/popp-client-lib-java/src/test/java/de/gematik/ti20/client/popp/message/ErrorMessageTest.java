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

class ErrorMessageTest {

  @Test
  void testDefaultConstructor() {
    ErrorMessage msg = new ErrorMessage();
    assertEquals(BasePoppMessageType.ERROR.getValue(), msg.getType());
    assertNull(msg.getErrorCode());
    assertNull(msg.getErrorDetail());
  }

  @Test
  void testConstructorWithParams() {
    ErrorMessage msg = new ErrorMessage("404", "Not Found");
    assertEquals(BasePoppMessageType.ERROR.getValue(), msg.getType());
    assertEquals("404", msg.getErrorCode());
    assertEquals("Not Found", msg.getErrorDetail());
  }

  @Test
  void testSettersAndGetters() {
    ErrorMessage msg = new ErrorMessage();
    msg.setErrorCode("500");
    msg.setErrorDetail("Internal Error");
    assertEquals("500", msg.getErrorCode());
    assertEquals("Internal Error", msg.getErrorDetail());
  }

  @Test
  void testToString() {
    ErrorMessage msg = new ErrorMessage("401", "Unauthorized");
    assertEquals("401 Unauthorized", msg.toString());
  }

  @Test
  void testJacksonSerializationAndDeserialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ErrorMessage msg = new ErrorMessage("403", "Forbidden");
    String json = mapper.writeValueAsString(msg);
    assertTrue(json.contains("\"errorCode\":\"403\""));
    assertTrue(json.contains("\"errorDetail\":\"Forbidden\""));
    assertTrue(json.contains("\"type\":\"" + BasePoppMessageType.ERROR.getValue() + "\""));

    ErrorMessage deserialized = mapper.readValue(json, ErrorMessage.class);
    assertEquals("403", deserialized.getErrorCode());
    assertEquals("Forbidden", deserialized.getErrorDetail());
    assertEquals(BasePoppMessageType.ERROR.getValue(), deserialized.getType());
  }
}
