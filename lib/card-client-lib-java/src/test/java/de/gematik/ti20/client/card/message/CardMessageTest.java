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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardMessageTest {

  @Test
  void testDefaultConstructor() {
    CardMessage msg = new CardMessage();
    assertNull(msg.getData());
  }

  @Test
  void testByteArrayConstructor() {
    byte[] data = "Hallo".getBytes(StandardCharsets.UTF_8);
    CardMessage msg = new CardMessage(data);
    assertArrayEquals(data, msg.getData());
    assertEquals("Hallo", msg.getText());
  }

  @Test
  void testStringConstructor() {
    String text = "Test123";
    CardMessage msg = new CardMessage(text);
    assertEquals(text, msg.getText());
    assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), msg.getData());
  }

  @Test
  void testObjectConstructorAndGetMapFromJson() throws IOException {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    CardMessage msg = new CardMessage(map);
    Map<?, ?> result = msg.getMapFromJson();
    assertEquals("bar", result.get("foo"));
  }

  @Test
  void testSetData() {
    CardMessage msg = new CardMessage();
    byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
    msg.setData(data);
    assertArrayEquals(data, msg.getData());
  }

  @Test
  void testGetJson() throws JsonProcessingException {
    CardMessage msg = new CardMessage("{foo: bar}");
    String json = msg.getJson();
    assertTrue(json.contains("{\"data\":\"e2ZvbzogYmFyfQ==\"}"));
  }

  @Test
  void testGetObjectFromJson() throws IOException {
    TestObj obj = new TestObj();
    obj.value = 42;
    CardMessage msg = new CardMessage(obj);
    TestObj result = msg.getObjectFromJson(TestObj.class);
    assertEquals(42, result.value);
  }

  static class TestObj {
    public int value;
  }
}
