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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardScenarioMessageTest {

  @Test
  void testDefaultConstructor() {
    StandardScenarioMessage msg = new StandardScenarioMessage();
    assertEquals(BasePoppMessageType.STANDARD_SCENARIO.getValue(), msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertNull(msg.getClientSessionId());
    assertEquals(0, msg.getSequenceCounter());
    assertEquals(0, msg.getTimeSpan());
    assertNull(msg.getSteps());
  }

  @Test
  void testConstructorWithParams() {
    List<ScenarioStep> steps =
        Arrays.asList(
            new ScenarioStep("00A40400", Arrays.asList("9000")),
            new ScenarioStep("00B00000", Arrays.asList("6A82")));
    StandardScenarioMessage msg = new StandardScenarioMessage("session123", 5, 42, steps);
    assertEquals(BasePoppMessageType.STANDARD_SCENARIO.getValue(), msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertEquals("session123", msg.getClientSessionId());
    assertEquals(5, msg.getSequenceCounter());
    assertEquals(42, msg.getTimeSpan());
    assertEquals(steps, msg.getSteps());
  }

  @Test
  void testSettersAndGetters() {
    StandardScenarioMessage msg = new StandardScenarioMessage();
    msg.setVersion("2.0.1");
    msg.setClientSessionId("abc");
    msg.setSequenceCounter(7);
    msg.setTimeSpan(99);
    List<ScenarioStep> steps = Arrays.asList(new ScenarioStep("00", Arrays.asList("9000")));
    msg.setSteps(steps);

    assertEquals("2.0.1", msg.getVersion());
    assertEquals("abc", msg.getClientSessionId());
    assertEquals(7, msg.getSequenceCounter());
    assertEquals(99, msg.getTimeSpan());
    assertEquals(steps, msg.getSteps());
  }

  @Test
  void testJacksonSerializationAndDeserialization() throws Exception {
    List<ScenarioStep> steps =
        Arrays.asList(new ScenarioStep("00A40400", Arrays.asList("9000", "6A82")));
    StandardScenarioMessage msg = new StandardScenarioMessage("sess", 1, 10, steps);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(msg);
    assertTrue(json.contains("\"version\":\"1.0.0\""));
    assertTrue(json.contains("\"clientSessionId\":\"sess\""));
    assertTrue(json.contains("\"sequenceCounter\":1"));
    assertTrue(json.contains("\"timeSpan\":10"));
    assertTrue(json.contains("\"steps\":["));

    StandardScenarioMessage deserialized = mapper.readValue(json, StandardScenarioMessage.class);
    assertEquals("1.0.0", deserialized.getVersion());
    assertEquals("sess", deserialized.getClientSessionId());
    assertEquals(1, deserialized.getSequenceCounter());
    assertEquals(10, deserialized.getTimeSpan());
    assertNotNull(deserialized.getSteps());
    assertEquals(1, deserialized.getSteps().size());
    assertEquals("00A40400", deserialized.getSteps().get(0).getCommandApdu());
    assertEquals(
        Arrays.asList("9000", "6A82"), deserialized.getSteps().get(0).getExpectedStatusWords());
  }
}
