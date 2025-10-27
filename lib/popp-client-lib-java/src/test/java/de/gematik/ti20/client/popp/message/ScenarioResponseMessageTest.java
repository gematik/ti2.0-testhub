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

class ScenarioResponseMessageTest {

  @Test
  void testDefaultConstructor() {
    ScenarioResponseMessage msg = new ScenarioResponseMessage();
    assertEquals(BasePoppMessageType.SCENARIO_RESPONSE.getValue(), msg.getType());
    assertNull(msg.getSteps());
  }

  @Test
  void testConstructorWithSteps() {
    List<String> steps = Arrays.asList("step1", "step2");
    ScenarioResponseMessage msg = new ScenarioResponseMessage(steps);
    assertEquals(BasePoppMessageType.SCENARIO_RESPONSE.getValue(), msg.getType());
    assertEquals(steps, msg.getSteps());
  }

  @Test
  void testSettersAndGetters() {
    ScenarioResponseMessage msg = new ScenarioResponseMessage();
    List<String> steps = Arrays.asList("a", "b", "c");
    msg.setSteps(steps);
    assertEquals(steps, msg.getSteps());
  }

  @Test
  void testJacksonSerializationAndDeserialization() throws Exception {
    List<String> steps = Arrays.asList("eins", "zwei");
    ScenarioResponseMessage msg = new ScenarioResponseMessage(steps);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(msg);
    assertTrue(json.contains("\"steps\":[\"eins\",\"zwei\"]"));
    assertTrue(
        json.contains("\"type\":\"" + BasePoppMessageType.SCENARIO_RESPONSE.getValue() + "\""));

    ScenarioResponseMessage deserialized = mapper.readValue(json, ScenarioResponseMessage.class);
    assertEquals(steps, deserialized.getSteps());
    assertEquals(BasePoppMessageType.SCENARIO_RESPONSE.getValue(), deserialized.getType());
  }
}
