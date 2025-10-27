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
package de.gematik.ti20.client.card.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardMessageExceptionTest {

  @Test
  void testConstructorAndGetters() {
    CardMessageException ex = new CardMessageException("TYPE1", "Fehlermeldung");
    assertEquals("TYPE1", ex.getType());
    assertEquals("Fehlermeldung", ex.getMessage());
    assertNull(ex.getDetails());

    CardMessageException ex2 = new CardMessageException("TYPE2", "Fehler", "Details");
    assertEquals("TYPE2", ex2.getType());
    assertEquals("Fehler", ex2.getMessage());
    assertEquals("Details", ex2.getDetails());
  }

  @Test
  void testToJson() throws Exception {
    CardMessageException ex = new CardMessageException("TYPE3", "Msg", "D");
    Map<String, String> json = ex.toJson();
    assertEquals("TYPE3", json.get("type"));
    assertEquals("Msg", json.get("message"));
    assertEquals("D", json.get("details"));
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    CardMessageException ex = new CardMessageException("TYPE4", "Nachricht", "Mehr Details");
    String jsonString = mapper.writeValueAsString(ex);

    assertTrue(jsonString.contains("\"type\":\"TYPE4\""));
    assertTrue(jsonString.contains("\"message\":\"Nachricht\""));
    assertTrue(jsonString.contains("\"details\":\"Mehr Details\""));

    CardMessageException deserialized = mapper.readValue(jsonString, CardMessageException.class);
    assertEquals("TYPE4", deserialized.getType());
    assertEquals("Nachricht", deserialized.getMessage());
    assertEquals("Mehr Details", deserialized.getDetails());
  }

  @Test
  void testJsonDeserializationWithoutDetails() throws Exception {
    String json = "{\"type\":\"TYPE5\",\"message\":\"Ohne Details\"}";
    ObjectMapper mapper = new ObjectMapper();
    CardMessageException ex = mapper.readValue(json, CardMessageException.class);
    assertEquals("TYPE5", ex.getType());
    assertEquals("Ohne Details", ex.getMessage());
    assertNull(ex.getDetails());
  }
}
