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
import de.gematik.ti20.client.card.exception.CardMessageException;
import org.junit.jupiter.api.Test;

class CardResultTest {

  @Test
  void testTypeEnumJson() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(CardResult.Type.CARD_DATA);
    assertEquals("\"card_data\"", json);

    CardResult.Type type = mapper.readValue("\"result\"", CardResult.Type.class);
    assertEquals(CardResult.Type.RESULT, type);
  }

  @Test
  void testEventEnumJson() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(CardResult.Event.CARD_INSERTED);
    assertEquals("\"cardInserted\"", json);

    CardResult.Event event = mapper.readValue("\"cardRemoved\"", CardResult.Event.class);
    assertEquals(CardResult.Event.CARD_REMOVED, event);
  }

  @Test
  void testConstructorAndGetters() {
    CardResult result = new CardResult(CardResult.Type.EVENT);
    assertEquals(CardResult.Type.EVENT, result.getType());
    assertNull(result.getResult());
    assertNull(result.getEvent());
    assertNull(result.getError());
  }

  @Test
  void testSettersAndFluentApi() {
    CardResult result = new CardResult(CardResult.Type.RESULT);
    result.setResult("foo");
    result.setEvent(CardResult.Event.CARD_INSERTED);
    CardMessageException ex = new CardMessageException("type", "msg");
    result.setError(ex);

    assertEquals("foo", result.getResult());
    assertEquals(CardResult.Event.CARD_INSERTED, result.getEvent());
    assertEquals(ex, result.getError());

    // Fluent API
    result.result("bar").event(CardResult.Event.CARD_REMOVED).error(ex);
    assertEquals("bar", result.getResult());
    assertEquals(CardResult.Event.CARD_REMOVED, result.getEvent());
    assertEquals(ex, result.getError());
  }

  @Test
  void testIsError() {
    CardResult errorResult = new CardResult(CardResult.Type.ERROR);
    assertTrue(errorResult.isError());

    CardResult okResult = new CardResult(CardResult.Type.RESULT);
    assertFalse(okResult.isError());
  }

  @Test
  void testStaticFactories() {
    CardMessageException ex = new CardMessageException("t", "m", "d");
    CardResult r1 = CardResult.newResult("abc");
    assertEquals(CardResult.Type.RESULT, r1.getType());
    assertEquals("abc", r1.getResult());

    CardResult r2 = CardResult.newEvent(CardResult.Event.CARD_INSERTED);
    assertEquals(CardResult.Type.EVENT, r2.getType());
    assertEquals(CardResult.Event.CARD_INSERTED, r2.getEvent());

    CardResult r3 = CardResult.newError(ex);
    assertEquals(CardResult.Type.ERROR, r3.getType());
    assertEquals(ex, r3.getError());

    CardResult r4 = CardResult.newError("t", "m");
    assertEquals(CardResult.Type.ERROR, r4.getType());
    assertEquals("t", r4.getError().getType());
    assertEquals("m", r4.getError().getMessage());

    CardResult r5 = CardResult.newError("t", "m", "d");
    assertEquals("d", r5.getError().getDetails());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    CardMessageException ex = new CardMessageException("t", "m", "d");
    CardResult original = new CardResult(CardResult.Type.ERROR);
    original.setError(ex);
    original.setResult("fail");
    original.setEvent(CardResult.Event.CARD_REMOVED);

    String json = mapper.writeValueAsString(original);
    CardResult deserialized = mapper.readValue(json, CardResult.class);

    assertEquals(CardResult.Type.ERROR, deserialized.getType());
    assertEquals("fail", deserialized.getResult());
    assertEquals(CardResult.Event.CARD_REMOVED, deserialized.getEvent());
    assertNotNull(deserialized.getError());
    assertEquals("t", deserialized.getError().getType());
    assertEquals("m", deserialized.getError().getMessage());
    assertEquals("d", deserialized.getError().getDetails());
  }
}
